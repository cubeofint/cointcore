package com.mawlee.cointcore.command;

import com.mawlee.cointcore.invsee.InvSeeDiscover;
import com.mawlee.cointcore.invsee.InvSeeService;
import com.mawlee.cointcore.invsee.InvSeeTarget;
import com.mawlee.cointcore.invsee.InvSeeTargetResolver;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InvSeeCommand {
    private InvSeeCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "invsee", invSeeCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> invSeeCommand() {
        return Commands.literal("invsee")
                .requires(InvSeeCommand::canInvSee)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(playerSuggestions())
                        .executes(ctx -> open(ctx, Section.PLAYER, null))
                        .then(Commands.literal("ender")
                                .executes(ctx -> open(ctx, Section.ENDER, null)))
                        .then(Commands.literal("curios")
                                .executes(ctx -> open(ctx, Section.CURIOS, null)))
                        .then(Commands.literal("cosmetic")
                                .executes(ctx -> open(ctx, Section.COSMETIC, null)))
                        .then(Commands.literal("pocket")
                                .executes(ctx -> open(ctx, Section.POCKET, null))
                                .then(Commands.argument("storage", StringArgumentType.word())
                                        .suggests(pocketSuggestions())
                                        .executes(ctx -> open(ctx, Section.POCKET,
                                                StringArgumentType.getString(ctx, "storage")))))
                        .then(Commands.literal("backpack")
                                .executes(ctx -> open(ctx, Section.BACKPACK, null))
                                .then(Commands.argument("backpack", StringArgumentType.greedyString())
                                        .suggests(backpackSuggestions())
                                        .executes(ctx -> open(ctx, Section.BACKPACK,
                                                StringArgumentType.getString(ctx, "backpack")))))
                        .then(Commands.literal("attachment")
                                .executes(ctx -> open(ctx, Section.ATTACHMENT, null))
                                .then(Commands.argument("key", StringArgumentType.greedyString())
                                        .suggests(attachmentSuggestions())
                                        .executes(ctx -> open(ctx, Section.ATTACHMENT,
                                                StringArgumentType.getString(ctx, "key"))))));
    }

    private static boolean canInvSee(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.INVSEE);
        }
        return source.hasPermission(2);
    }

    private static int open(CommandContext<CommandSourceStack> context, Section section, String arg) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer viewer)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_PLAYER_ONLY));
            return 0;
        }

        String targetName = StringArgumentType.getString(context, "target");
        Optional<InvSeeTarget> target = InvSeeTargetResolver.resolve(source.getServer(), targetName);
        if (target.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_PLAYER_NOT_FOUND, targetName));
            return 0;
        }

        InvSeeTarget resolved = target.get();
        if (resolved.playerId().equals(viewer.getUUID())) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_CANNOT_SELF));
            return 0;
        }

        boolean opened = switch (section) {
            case PLAYER -> InvSeeService.openPlayer(viewer, resolved);
            case ENDER -> InvSeeService.openEnder(viewer, resolved);
            case CURIOS -> InvSeeService.openCurios(viewer, resolved);
            case COSMETIC -> InvSeeService.openCosmetic(viewer, resolved);
            case POCKET -> openPocket(viewer, resolved, arg);
            case BACKPACK -> openBackpack(viewer, resolved, arg);
            case ATTACHMENT -> openAttachment(viewer, resolved, arg);
        };

        if (opened) {
            source.sendSuccess(
                    () -> resolved.isOffline()
                            ? CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_OPENED_OFFLINE, resolved.displayName())
                            : CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_OPENED_ONLINE, resolved.displayName()),
                    false
            );
            return 1;
        }

        source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_FAILED, resolved.displayName()));
        return 0;
    }

    private static boolean openPocket(ServerPlayer viewer, InvSeeTarget target, String arg) {
        UUID storageId;
        if (arg == null || arg.isBlank()) {
            storageId = InvSeeService.resolveDefaultPocket(target);
        } else {
            try {
                storageId = UUID.fromString(arg);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return storageId != null && InvSeeService.openPocket(viewer, target, storageId);
    }

    private static boolean openBackpack(ServerPlayer viewer, InvSeeTarget target, String arg) {
        String key = arg;
        if (key == null || key.isBlank()) {
            key = InvSeeService.resolveDefaultBackpack(target);
        } else if (key.chars().allMatch(Character::isDigit)) {
            List<String> keys = InvSeeDiscover.backpackKeys(target.getPlayer());
            int index = Integer.parseInt(key);
            if (index < 0 || index >= keys.size()) {
                return false;
            }
            key = keys.get(index);
        }
        return key != null && InvSeeService.openBackpack(viewer, target, key);
    }

    private static boolean openAttachment(ServerPlayer viewer, InvSeeTarget target, String arg) {
        String key = arg;
        if (key == null || key.isBlank()) {
            key = InvSeeService.resolveDefaultAttachment(target, viewer);
        }
        return key != null && InvSeeService.openAttachment(viewer, target, key);
    }

    private static SuggestionProvider<CommandSourceStack> playerSuggestions() {
        return (context, builder) -> SharedSuggestionProvider.suggest(suggestPlayerNames(context.getSource()), builder);
    }

    private static SuggestionProvider<CommandSourceStack> pocketSuggestions() {
        return (context, builder) -> {
            Optional<InvSeeTarget> target = resolveTarget(context);
            if (target.isEmpty() || !ModList.get().isLoaded("pocketstorage")) {
                return builder.buildFuture();
            }
            return SharedSuggestionProvider.suggest(InvSeeDiscover.pocketIds(target.get().getPlayer()), builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> backpackSuggestions() {
        return (context, builder) -> {
            Optional<InvSeeTarget> target = resolveTarget(context);
            if (target.isEmpty() || !ModList.get().isLoaded("sophisticatedbackpacks")) {
                return builder.buildFuture();
            }
            List<String> keys = InvSeeDiscover.backpackKeys(target.get().getPlayer());
            List<String> suggestions = new ArrayList<>(keys);
            for (int i = 0; i < keys.size(); i++) {
                suggestions.add(Integer.toString(i));
            }
            return SharedSuggestionProvider.suggest(suggestions, builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> attachmentSuggestions() {
        return (context, builder) -> {
            Optional<InvSeeTarget> target = resolveTarget(context);
            if (target.isEmpty() || !(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                return builder.buildFuture();
            }
            return SharedSuggestionProvider.suggest(
                    InvSeeDiscover.attachmentKeys(target.get().getPlayer(), viewer.registryAccess()),
                    builder
            );
        };
    }

    private static Optional<InvSeeTarget> resolveTarget(CommandContext<CommandSourceStack> context) {
        try {
            String name = StringArgumentType.getString(context, "target");
            return InvSeeTargetResolver.resolve(context.getSource().getServer(), name);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private enum Section {
        PLAYER,
        ENDER,
        CURIOS,
        COSMETIC,
        POCKET,
        BACKPACK,
        ATTACHMENT
    }
}
