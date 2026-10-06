package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.ShopMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TraderClientSetup {
    private TraderClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ShopMenus.TRADER.get(), TraderScreen::new);
    }
}
