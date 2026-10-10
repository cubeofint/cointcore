package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketRestockTest {
    @Test
    void acceptCountStopsAtCapAndRejectsEmpty() {
        assertEquals(0, MarketRestock.acceptCount(10L, 10L, 64));
        assertEquals(0, MarketRestock.acceptCount(0L, 0L, 8));
        assertEquals(0, MarketRestock.acceptCount(5L, 20L, 0));
        assertEquals(10, MarketRestock.acceptCount(10L, 20L, 64));
        assertEquals(3, MarketRestock.acceptCount(0L, 100L, 3));
        assertEquals(1, MarketRestock.acceptCount(GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT - 1L,
                GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT, 8));
    }
}
