package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.shop.GluonWallet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BalanceCommand {
    private BalanceCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher, "balance");
        register(dispatcher, "money");
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(
                Commands.literal(name)
                        .requires(BalanceCommand::canUse)
                        .executes(BalanceCommand::show)
        );
    }

    private static boolean canUse(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.GLUONS_BALANCE);
        }
        return false;
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        long balance = GluonWallet.get(player);
        context.getSource().sendSuccess(
                () -> CointCoreMessages.forPlayer(player, CointCoreMessages.GLUONS_BALANCE, balance),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
