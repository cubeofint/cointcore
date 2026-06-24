package com.mawlee.cointcore.command;

import com.mawlee.cointcore.kit.KitCreditService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftbessentials.kit.KitManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class KitCreditCommand {
    private static final SimpleCommandExceptionType FTB_UNAVAILABLE = new SimpleCommandExceptionType(
            Component.literal("FTB Essentials is not loaded")
    );
    private static final SimpleCommandExceptionType INVALID_AMOUNT = new SimpleCommandExceptionType(
            Component.literal("Invalid amount")
    );

    private KitCreditCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> kitCreditCommand() {
        return Commands.literal("kit")
                .then(Commands.literal("balance")
                        .executes(KitCreditCommand::showOwnBalance)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .requires(KitCreditCommand::canManageCredits)
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .executes(KitCreditCommand::showTargetBalance)))
                .then(Commands.literal("add")
                        .requires(KitCreditCommand::canManageCredits)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("kit", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestKitNames(builder))
                                        .then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(KitCreditCommand::addCredits)))))
                .then(Commands.literal("set")
                        .requires(KitCreditCommand::canManageCredits)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("kit", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestKitNames(builder))
                                        .then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(KitCreditCommand::setCredits)))))
                .then(Commands.literal("take")
                        .requires(KitCreditCommand::canManageCredits)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("kit", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestKitNames(builder))
                                        .then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(KitCreditCommand::takeCredits)))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> playerKitBalanceCommand() {
        return Commands.literal("kit")
                .then(Commands.literal("balance")
                        .executes(KitCreditCommand::showOwnBalance));
    }

    private static boolean canManageCredits(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.KIT_CREDITS);
        }

        return source.hasPermission(2);
    }

    private static int showOwnBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showBalance(context.getSource(), player.getUUID(), player.getGameProfile().getName());
    }

    private static int showTargetBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");
        ResolvedTarget target = resolveTarget(source, targetName);
        return showBalance(source, target.id(), target.name());
    }

    private static int showBalance(CommandSourceStack source, UUID playerId, String playerName) {
        Map<String, Integer> credits = KitCreditService.getAllCredits(source.getServer(), playerId);
        if (credits.isEmpty()) {
            source.sendSuccess(() -> messageFor(source, CointCoreMessages.KIT_CREDIT_BALANCE_EMPTY, playerName), false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> messageFor(source, CointCoreMessages.KIT_CREDIT_BALANCE_HEADER, playerName), false);
        credits.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> source.sendSuccess(
                        () -> messageFor(source, CointCoreMessages.KIT_CREDIT_BALANCE_ENTRY, entry.getKey(), entry.getValue()),
                        false
                ));
        return Command.SINGLE_SUCCESS;
    }

    private static int addCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "target"));
        String kitName = StringArgumentType.getString(context, "kit");
        int amount = parseAmount(context, 1);

        KitCreditService.ensureKitExists(kitName);
        int total = KitCreditService.addCredits(source.getServer(), target.id(), kitName, amount);
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.KIT_CREDIT_ADDED, amount, kitName, target.name(), total),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int setCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "target"));
        String kitName = StringArgumentType.getString(context, "kit");
        int amount = parseAmount(context, 0);

        KitCreditService.ensureKitExists(kitName);
        int total = KitCreditService.setCredits(source.getServer(), target.id(), kitName, amount);
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.KIT_CREDIT_SET, kitName, target.name(), total),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int takeCredits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ensureFtbLoaded();
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "target"));
        String kitName = StringArgumentType.getString(context, "kit");
        int amount = parseAmount(context, 1);

        KitCreditService.ensureKitExists(kitName);
        int total = KitCreditService.takeCredits(source.getServer(), target.id(), kitName, amount);
        source.sendSuccess(
                () -> messageFor(source, CointCoreMessages.KIT_CREDIT_TAKEN, amount, kitName, target.name(), total),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int parseAmount(CommandContext<CommandSourceStack> context, int min) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, "amount");
        int end = 0;
        while (end < raw.length() && Character.isDigit(raw.charAt(end))) {
            end++;
        }
        if (end == 0) {
            throw INVALID_AMOUNT.create();
        }

        int value = Integer.parseInt(raw.substring(0, end));
        if (value < min) {
            throw INVALID_AMOUNT.create();
        }
        return value;
    }

    private static ResolvedTarget resolveTarget(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            throw playerNotFound(targetName);
        }

        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        return new ResolvedTarget(targetId.get(), resolvedName);
    }

    private static CommandSyntaxException playerNotFound(String targetName) {
        return new SimpleCommandExceptionType(
                CointCoreMessages.forConsole(CointCoreMessages.KIT_CREDIT_PLAYER_NOT_FOUND, targetName)
        ).create();
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private record ResolvedTarget(UUID id, String name) {
    }

    private static void ensureFtbLoaded() throws CommandSyntaxException {
        try {
            Class.forName("dev.ftb.mods.ftbessentials.kit.KitManager");
        } catch (ClassNotFoundException exception) {
            throw FTB_UNAVAILABLE.create();
        }
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestKitNames(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                KitManager.getInstance().allKits().stream().map(Kit::getKitName).toList(),
                builder
        );
    }

    private static Component messageFor(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return CointCoreMessages.forPlayer(player, key, args);
        }

        return CointCoreMessages.forConsole(key, args);
    }
}
