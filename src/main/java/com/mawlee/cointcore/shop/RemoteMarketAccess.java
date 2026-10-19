package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Remote (inventory-button) access to the global player-shop catalog.
 * Stock is read from {@link GlobalMarketSavedData}; chunks are never loaded.
 */
public final class RemoteMarketAccess {
    private RemoteMarketAccess() {
    }

    public static boolean allowed(boolean configEnabled, boolean permission) {
        return configEnabled && permission;
    }

    public static boolean canUse(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return allowed(
                TraderOffersConfig.remoteMarketEnabled(),
                PermissionService.has(player, CointPermissionNodes.MARKET_REMOTE)
        );
    }
}
