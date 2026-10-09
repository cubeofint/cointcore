package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.PlayerTraderCatalogPayload;
import com.mawlee.cointcore.shop.PlayerTraderManageMenu;
import com.mawlee.cointcore.shop.PlayerTraderManageSyncPayload;
import com.mawlee.cointcore.shop.PlayerTraderMenu;
import net.minecraft.client.Minecraft;

public final class PlayerTraderClientSync {
    private PlayerTraderClientSync() {
    }

    public static void applyCatalog(PlayerTraderCatalogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof PlayerTraderMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.refresh(payload.balance(), payload.ownerName(), payload.listings());
        if (minecraft.screen instanceof PlayerTraderScreen screen) {
            screen.onCatalogUpdated();
        }
    }

    public static void applyManage(PlayerTraderManageSyncPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof PlayerTraderManageMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.refresh(payload.ownerName(), payload.lifetimeRevenue(), payload.offers());
        if (minecraft.screen instanceof PlayerTraderManageScreen screen) {
            screen.onOffersUpdated(payload.lastActionOk());
        }
    }
}
