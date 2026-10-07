package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.server.MinecraftServer;

/**
 * Periodically samples current buy prices into {@link TraderPriceHistorySavedData}.
 */
public final class TraderPriceHistorySampler {
    private static int ticks;

    private TraderPriceHistorySampler() {
    }

    public static void tick(MinecraftServer server) {
        int interval = TraderOffersConfig.priceHistorySampleIntervalTicks();
        ticks++;
        if (ticks < interval) {
            return;
        }
        ticks = 0;
        TraderPriceHistorySavedData.get(server).sampleOffers(TraderOffersConfig.offers());
    }
}
