package com.mawlee.cointcore.command;

import com.mawlee.cointcore.nightvision.NightVisionService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NightVisionCommand {
    private NightVisionCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("nv")
                        .requires(NightVisionCommand::canUse)
                        .executes(NightVisionCommand::toggle)
        );
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, CointPermissionNodes.NIGHT_VISION);
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NightVisionService.toggle(player);
        return 1;
    }
}
