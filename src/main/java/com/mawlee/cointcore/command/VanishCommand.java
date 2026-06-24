package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.vanish.VanishService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class VanishCommand {
    private VanishCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var vanish = Commands.literal("vanish")
                .requires(VanishCommand::canUseVanish)
                .executes(VanishCommand::toggleVanish)
                .then(Commands.literal("mobs")
                        .requires(VanishCommand::canUseVanishMobs)
                        .executes(VanishCommand::toggleMobs));

        dispatcher.register(vanish);
        dispatcher.register(Commands.literal("v")
                .requires(VanishCommand::canUseVanish)
                .executes(VanishCommand::toggleVanish)
                .then(Commands.literal("mobs")
                        .requires(VanishCommand::canUseVanishMobs)
                        .executes(VanishCommand::toggleMobs)));
    }

    private static boolean canUseVanish(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, CointPermissionNodes.VANISH);
    }

    private static boolean canUseVanishMobs(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, CointPermissionNodes.VANISH_MOBS);
    }

    private static int toggleVanish(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        VanishService.toggle(player);
        return 1;
    }

    private static int toggleMobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        VanishService.toggleMobs(player);
        return 1;
    }
}
