package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class PlayerShopAccess {
    private PlayerShopAccess() {
    }

    public static boolean canPlace(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.PLAYER_SHOP_PLACE);
    }

    public static boolean canUse(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.PLAYER_SHOP_USE);
    }

    public static boolean canAdmin(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.PLAYER_SHOP_ADMIN);
    }

    public static boolean canManage(Player player, UUID ownerId) {
        if (player == null || ownerId == null) {
            return false;
        }
        if (ownerId.equals(player.getUUID())) {
            return true;
        }
        return player instanceof ServerPlayer serverPlayer && canAdmin(serverPlayer);
    }
}
