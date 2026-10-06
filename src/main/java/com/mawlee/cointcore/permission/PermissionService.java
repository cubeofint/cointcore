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

    public static int integer(ServerPlayer player, PermissionNode<Integer> node, int fallback) {
        try {
            Integer value = PermissionAPI.getPermission(player, node);
            return value != null ? value : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
