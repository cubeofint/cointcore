package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
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
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
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
        long balance = GluonWallet.set(source.getServer(), target.id(), amount);
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
        long balance = GluonWallet.add(source.getServer(), target.id(), amount);
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
