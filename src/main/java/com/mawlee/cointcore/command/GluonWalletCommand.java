package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.shop.CurrencyMovement;
import com.mawlee.cointcore.shop.CurrencyMovementService;
import com.mawlee.cointcore.shop.CurrencyMovementType;
import com.mawlee.cointcore.shop.GluonWallet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GluonWalletCommand {
    private GluonWalletCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("gluons")
                .requires(GluonWalletCommand::canAdmin)
                .then(Commands.literal("get")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .executes(GluonWalletCommand::getBalance)))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(GluonWalletCommand::setBalance))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("amount", LongArgumentType.longArg())
                                        .executes(GluonWalletCommand::addBalance))));
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.GLUONS_ADMIN);
        }
        return source.hasPermission(2);
    }

    private static int getBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "player"));
        long balance = GluonWallet.get(source.getServer(), target.id());
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.GLUONS_GET, target.name(), balance),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int setBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "player"));
        long amount = LongArgumentType.getLong(context, "amount");
        long previous = GluonWallet.get(source.getServer(), target.id());
        long balance = GluonWallet.set(source.getServer(), target.id(), amount);
        long delta = balance - previous;
        CurrencyMovementService.record(
                source.getServer(),
                operatorId(source),
                source.getTextName(),
                target.id(),
                target.name(),
                Math.abs(delta),
                CurrencyMovementType.ADMIN_SET,
                "previous=" + previous,
                List.of(new CurrencyMovement.Delta(target.id(), delta, balance)),
                null
        );
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.GLUONS_SET, target.name(), balance),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int addBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResolvedTarget target = resolveTarget(source, StringArgumentType.getString(context, "player"));
        long amount = LongArgumentType.getLong(context, "amount");
        long balance;
        if (amount > 0L) {
            balance = GluonWallet.add(source.getServer(), target.id(), amount);
        } else if (amount < 0L) {
            long debit = amount == Long.MIN_VALUE ? Long.MAX_VALUE : -amount;
            if (!GluonWallet.trySubtract(source.getServer(), target.id(), debit)) {
                throw new SimpleCommandExceptionType(
                        CointCoreMessages.forConsole(CointCoreMessages.GLUONS_PAY_NOT_ENOUGH, debit)
                ).create();
            }
            balance = GluonWallet.get(source.getServer(), target.id());
        } else {
            balance = GluonWallet.get(source.getServer(), target.id());
        }
        CurrencyMovementService.record(
                source.getServer(),
                operatorId(source),
                source.getTextName(),
                target.id(),
                target.name(),
                Math.abs(amount == Long.MIN_VALUE ? Long.MAX_VALUE : amount),
                CurrencyMovementType.ADMIN_ADD,
                "balance=" + balance,
                List.of(new CurrencyMovement.Delta(target.id(), amount == Long.MIN_VALUE ? -Long.MAX_VALUE : amount, balance)),
                null
        );
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.GLUONS_ADD, amount, target.name(), balance),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static ResolvedTarget resolveTarget(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        Optional<UUID> targetId = MuteService.resolvePlayerId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forConsole(CointCoreMessages.GLUONS_PLAYER_NOT_FOUND, targetName)
            ).create();
        }
        String resolvedName = MuteService.resolveName(source.getServer(), targetId.get()).orElse(targetName);
        return new ResolvedTarget(targetId.get(), resolvedName);
    }

    private static UUID operatorId(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null;
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
}
