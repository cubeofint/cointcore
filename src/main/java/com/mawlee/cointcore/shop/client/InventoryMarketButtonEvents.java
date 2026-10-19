package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.MarketButtonConfig;
import com.mawlee.cointcore.shop.RemoteMarketOpenPayload;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class InventoryMarketButtonEvents {
    private InventoryMarketButtonEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            addButton(event, MarketButtonConfig.inventoryX(), MarketButtonConfig.inventoryY());
            return;
        }
        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            addButton(event, MarketButtonConfig.creativeX(), MarketButtonConfig.creativeY());
        }
    }

    private static void addButton(ScreenEvent.Init.Post event, int x, int y) {
        event.addListener(new InventoryMarketButton(x, y, button ->
                PacketDistributor.sendToServer(new RemoteMarketOpenPayload())
        ));
    }
}
