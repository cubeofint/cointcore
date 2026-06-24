package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.pvp.PvpModeService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class TurnPvpCommand {
    private TurnPvpCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "turn-pvp", turnPvpCommand());
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> turnPvpCommand() {
        return Commands.literal("turn-pvp")
                .requires(TurnPvpCommand::canUse)
                .executes(TurnPvpCommand::toggle);
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player
                && PermissionService.has(player, CointPermissionNodes.TURN_PVP);
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PvpModeService.toggle(player);
        return 1;
    }
}
