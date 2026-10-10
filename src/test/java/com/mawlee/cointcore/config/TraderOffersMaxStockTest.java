package com.mawlee.cointcore.config;

import com.mawlee.cointcore.shop.GlobalMarketMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraderOffersMaxStockTest {
    @Test
    void defaultsAndClampsMaxStockPerLot() {
        TraderOffersConfig.FileData data = TraderOffersConfig.FileData.defaults();
        assertEquals(GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT, TraderOffersConfig.parse(data).playerShopMaxStockPerLot());
        data.playerShopMaxStockPerLot = -4L;
        assertEquals(0L, TraderOffersConfig.parse(data).playerShopMaxStockPerLot());
        data.playerShopMaxStockPerLot = 50L;
        assertEquals(50L, TraderOffersConfig.parse(data).playerShopMaxStockPerLot());
    }
}
