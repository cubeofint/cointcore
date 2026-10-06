package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.shop.CurrencyMovementService;
import com.mawlee.cointcore.shop.CurrencyMovementType;
import com.mawlee.cointcore.shop.GluonTransfer;
import com.mawlee.cointcore.shop.GluonWallet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public final class PayCommand {
    private PayCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher, "pay");
        register(dispatcher, "transfer");
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(
                Commands.literal(name)
                        .requires(PayCommand::canUse)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestPlayerNames(context.getSource()),
                                        builder
                                ))
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(PayCommand::pay)))
        );
    }

    private static boolean canUse(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.GLUONS_PAY);
        }
        return false;
    }

    private static int pay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        Optional<UUID> targetId = MuteService.resolvePlayerId(sender.server, targetName);
        if (targetId.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forPlayer(sender, CointCoreMessages.GLUONS_PLAYER_NOT_FOUND, targetName)
            ).create();
        }
        String resolvedName = MuteService.resolveName(sender.server, targetId.get()).orElse(targetName);
        if (!GluonTransfer.canTransfer(sender.getUUID(), targetId.get(), amount)) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forPlayer(sender, CointCoreMessages.GLUONS_PAY_SELF)
            ).create();
        }
        if (!GluonWallet.tryTransfer(sender.server, sender.getUUID(), targetId.get(), amount)) {
            throw new SimpleCommandExceptionType(
                    CointCoreMessages.forPlayer(sender, CointCoreMessages.GLUONS_PAY_NOT_ENOUGH, amount)
            ).create();
        }
        CurrencyMovementService.record(
                sender.server,
                sender.getUUID(),
                sender.getGameProfile().getName(),
                targetId.get(),
                resolvedName,
                amount,
                CurrencyMovementType.PAY,
                null
        );
        context.getSource().sendSuccess(
                () -> CointCoreMessages.forPlayer(sender, CointCoreMessages.GLUONS_PAY_SENT, amount, resolvedName),
                false
        );
        ServerPlayer onlineTarget = sender.server.getPlayerList().getPlayer(targetId.get());
        if (onlineTarget != null) {
            onlineTarget.sendSystemMessage(
                    CointCoreMessages.forPlayer(onlineTarget, CointCoreMessages.GLUONS_PAY_RECEIVED, amount, sender.getGameProfile().getName())
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }
}
