package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.privilege.PrivilegeCommandService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public final class PrivilegeCommands {
    private PrivilegeCommands() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("repair")
                        .requires(source -> canUse(source, CointPermissionNodes.REPAIR))
                        .executes(PrivilegeCommands::repair)
        );
        dispatcher.register(
                Commands.literal("charge")
                        .requires(source -> canUse(source, CointPermissionNodes.CHARGE))
                        .executes(PrivilegeCommands::charge)
        );
        dispatcher.register(
                Commands.literal("repairall")
                        .requires(source -> canUse(source, CointPermissionNodes.REPAIR_ALL))
                        .executes(PrivilegeCommands::repairAll)
        );
        dispatcher.register(
                Commands.literal("chargeall")
                        .requires(source -> canUse(source, CointPermissionNodes.CHARGE_ALL))
                        .executes(PrivilegeCommands::chargeAll)
        );
        dispatcher.register(
                Commands.literal("expadd")
                        .requires(source -> canUse(source, CointPermissionNodes.EXP_ADD))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(PrivilegeCommands::expAdd))
        );
    }

    private static boolean canUse(CommandSourceStack source, PermissionNode<Boolean> permission) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, permission);
    }

    private static int repair(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return PrivilegeCommandService.repairMainHand(context.getSource().getPlayerOrException());
    }

    private static int charge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return PrivilegeCommandService.chargeMainHand(context.getSource().getPlayerOrException());
    }

    private static int repairAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return PrivilegeCommandService.repairAll(context.getSource().getPlayerOrException());
    }

    private static int chargeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return PrivilegeCommandService.chargeAll(context.getSource().getPlayerOrException());
    }

    private static int expAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return PrivilegeCommandService.addExperience(player, amount);
    }
}
