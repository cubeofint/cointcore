package com.mawlee.cointcore.permission;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public final class PermissionService {
    private PermissionService() {
    }

    public static boolean has(ServerPlayer player, PermissionNode<Boolean> node) {
        try {
            return PermissionAPI.getPermission(player, node);
        } catch (RuntimeException exception) {
            return player.hasPermissions(2);
        }
    }
}
