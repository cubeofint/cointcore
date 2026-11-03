package com.mawlee.cointcore.command;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/** Per-mode self permissions, without granting vanilla /gamemode to every mode and target. */
public final class StaffModeCommand {
    private StaffModeCommand() { }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("staffmode");
        for (GameType mode : GameType.values()) {
            root.then(Commands.literal(mode.getName())
                    .requires(source -> source.getEntity() instanceof ServerPlayer player
                            && PermissionService.has(player, permission(mode)))
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();
                        player.setGameMode(mode);
                        context.getSource().sendSuccess(() -> Component.literal("Режим: " + mode.getName()), true);
                        return 1;
                    }));
        }
        CommandRegistration.replaceRootLiteral(dispatcher, "staffmode", root);
    }

    private static PermissionNode<Boolean> permission(GameType mode) {
        return switch (mode) {
            case SURVIVAL -> CointPermissionNodes.MODE_SURVIVAL;
            case SPECTATOR -> CointPermissionNodes.MODE_SPECTATOR;
            case CREATIVE -> CointPermissionNodes.MODE_CREATIVE;
            case ADVENTURE -> CointPermissionNodes.MODE_ADVENTURE;
        };
    }
}
