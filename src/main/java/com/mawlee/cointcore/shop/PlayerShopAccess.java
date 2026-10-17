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

    public static boolean isAdmin(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return canAdmin(serverPlayer);
        }
        return player != null && ShopBreakPolicy.clientOperator(player.hasPermissions(2));
    }

    public static boolean canManage(Player player, UUID ownerId) {
        if (player == null) {
            return false;
        }
        String owner = ownerId == null ? null : ownerId.toString();
        return PlayerShopManagementPolicy.canManage(player.getUUID().toString(), owner, isAdmin(player));
    }

    public static boolean canBreakNpcTrader(Player player) {
        return ShopBreakPolicy.allowNpcTraderBreak(isAdmin(player));
    }

    public static boolean allowOpenManage(boolean requestedManage, Player player, UUID ownerId) {
        return PlayerShopManagementPolicy.allowOpenManage(requestedManage, canManage(player, ownerId));
    }
}
