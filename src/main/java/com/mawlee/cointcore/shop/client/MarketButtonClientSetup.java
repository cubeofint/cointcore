package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.MarketButtonConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MarketButtonClientSetup {
    private MarketButtonClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MarketButtonConfig.load();
    }
}
