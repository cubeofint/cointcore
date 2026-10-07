package com.mawlee.cointcore.command;

import com.mawlee.cointcore.invsee.InvSeeAuditLog;
import com.mawlee.cointcore.invsee.InvSeeDiscover;
import com.mawlee.cointcore.invsee.InvSeeNameSuggestions;
import com.mawlee.cointcore.invsee.InvSeePermissions;
import com.mawlee.cointcore.invsee.InvSeeSection;
import com.mawlee.cointcore.invsee.InvSeeService;
import com.mawlee.cointcore.invsee.InvSeeTarget;
import com.mawlee.cointcore.invsee.InvSeeTargetResolver;
import com.mawlee.cointcore.invsee.InvSeeTargets;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
                        .executes(ctx -> open(ctx, InvSeeSection.INVENTORY, null))
                        .then(Commands.literal("ender")
                                .executes(ctx -> open(ctx, InvSeeSection.ENDER, null)))
                        .then(Commands.literal("curios")
                                .executes(ctx -> open(ctx, InvSeeSection.CURIOS, null)))
                        .then(Commands.literal("cosmetic")
                                .executes(ctx -> open(ctx, InvSeeSection.COSMETIC, null)))
                        .then(Commands.literal("pocket")
                                .executes(ctx -> open(ctx, InvSeeSection.POCKET, null))
                                .then(Commands.argument("storage", StringArgumentType.word())
                                        .suggests(pocketSuggestions())
                                        .executes(ctx -> open(ctx, InvSeeSection.POCKET,
                                                StringArgumentType.getString(ctx, "storage")))))
                        .then(Commands.literal("backpack")
                                .executes(ctx -> open(ctx, InvSeeSection.BACKPACK, null))
                                .then(Commands.argument("backpack", StringArgumentType.greedyString())
                                        .suggests(backpackSuggestions())
                                        .executes(ctx -> open(ctx, InvSeeSection.BACKPACK,
                                                StringArgumentType.getString(ctx, "backpack")))))
                        .then(Commands.literal("accessories")
                                .executes(ctx -> open(ctx, InvSeeSection.ACCESSORIES, null)))
                        .then(Commands.literal("ftb")
                                .executes(ctx -> open(ctx, InvSeeSection.FTB, null)))
                        .then(Commands.literal("graves")
                                .executes(ctx -> open(ctx, InvSeeSection.GRAVES, null)))
                        .then(Commands.literal("state")
                                .executes(ctx -> open(ctx, InvSeeSection.STATE, null)))
                        .then(Commands.literal("attachment")
                                .executes(ctx -> open(ctx, InvSeeSection.MODDATA, null))
                                .then(Commands.argument("key", StringArgumentType.greedyString())
                                        .suggests(attachmentSuggestions())
                                        .executes(ctx -> open(ctx, InvSeeSection.MODDATA,
                                                StringArgumentType.getString(ctx, "key"))))));
    }

    private static boolean canInvSee(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return InvSeePermissions.canUseCommand(player);
        }
        return source.hasPermission(2);
    }

    private static int open(CommandContext<CommandSourceStack> context, InvSeeSection section, String arg) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer viewer)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_PLAYER_ONLY));
            return 0;
        }

        String targetName = StringArgumentType.getString(context, "target");
        Optional<InvSeeTarget> target = InvSeeTargetResolver.acquire(source.getServer(), targetName);
        if (target.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_PLAYER_NOT_FOUND, targetName));
            return 0;
        }

        InvSeeTarget resolved = target.get();
        if (resolved.playerId().equals(viewer.getUUID())) {
            InvSeeTargets.release(resolved);
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_CANNOT_SELF));
            return 0;
        }

        if (resolved.isOffline() && !InvSeePermissions.canOpenOffline(viewer)) {
            InvSeeTargets.release(resolved);
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_NO_OFFLINE));
            return 0;
        }

        if (!InvSeePermissions.canInspect(viewer, source.getServer(), resolved.playerId())) {
            InvSeeTargets.release(resolved);
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_EXEMPT, resolved.displayName()));
            return 0;
        }

        if (!InvSeePermissions.canView(viewer, section)) {
            InvSeeTargets.release(resolved);
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_NO_SECTION, section.id()));
            return 0;
        }

        boolean opened = switch (section) {
            case INVENTORY -> InvSeeService.openPlayer(viewer, resolved);
            case ENDER -> InvSeeService.openEnder(viewer, resolved);
            case CURIOS -> InvSeeService.openCurios(viewer, resolved);
            case COSMETIC -> InvSeeService.openCosmetic(viewer, resolved);
            case POCKET -> openPocket(viewer, resolved, arg);
            case BACKPACK -> openBackpack(viewer, resolved, arg);
            case MODDATA -> openAttachment(viewer, resolved, arg);
            case ACCESSORIES -> InvSeeService.openAccessories(viewer, resolved);
            case STATE, FTB, GRAVES -> InvSeeService.openInfo(viewer, resolved, section);
        };

        if (opened) {
            InvSeeAuditLog.opened(
                    viewer.getGameProfile().getName(),
                    resolved.displayName(),
                    section,
                    !resolved.isOffline(),
                    InvSeePermissions.canEdit(viewer, section)
            );
            if (resolved.editLock().isHeld() && !resolved.editLock().isHeldBy(viewer.getUUID())) {
                source.sendSuccess(
                        () -> CointCoreMessages.forSource(
                                source,
                                CointCoreMessages.INVSEE_BUSY,
                                resolved.editLock().editorName()
                        ),
                        false
                );
            }
            source.sendSuccess(
                    () -> resolved.isOffline()
                            ? CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_OPENED_OFFLINE, resolved.displayName())
                            : CointCoreMessages.forSource(source, CointCoreMessages.INVSEE_OPENED_ONLINE, resolved.displayName()),
                    false
            );
            return 1;
        }

        InvSeeTargets.release(resolved);
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
        return (context, builder) -> SharedSuggestionProvider.suggest(
                InvSeeNameSuggestions.suggest(context.getSource().getServer()),
                builder
        );
    }

    private static SuggestionProvider<CommandSourceStack> pocketSuggestions() {
        return (context, builder) -> {
            Optional<Player> target = suggestionPlayer(context);
            if (target.isEmpty() || !ModList.get().isLoaded("pocketstorage")) {
                return builder.buildFuture();
            }
            return SharedSuggestionProvider.suggest(InvSeeDiscover.pocketIds(target.get()), builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> backpackSuggestions() {
        return (context, builder) -> {
            Optional<Player> target = suggestionPlayer(context);
            if (target.isEmpty() || !ModList.get().isLoaded("sophisticatedbackpacks")) {
                return builder.buildFuture();
            }
            List<String> keys = InvSeeDiscover.backpackKeys(target.get());
            List<String> suggestions = new ArrayList<>(keys);
            for (int i = 0; i < keys.size(); i++) {
                suggestions.add(Integer.toString(i));
            }
            return SharedSuggestionProvider.suggest(suggestions, builder);
        };
    }

    private static SuggestionProvider<CommandSourceStack> attachmentSuggestions() {
        return (context, builder) -> {
            Optional<Player> target = suggestionPlayer(context);
            if (target.isEmpty() || !(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                return builder.buildFuture();
            }
            return SharedSuggestionProvider.suggest(
                    InvSeeDiscover.attachmentKeys(target.get(), viewer.registryAccess()),
                    builder
            );
        };
    }

    private static Optional<Player> suggestionPlayer(CommandContext<CommandSourceStack> context) {
        try {
            String name = StringArgumentType.getString(context, "target");
            return InvSeeTargetResolver.suggestionPlayer(context.getSource().getServer(), name);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
