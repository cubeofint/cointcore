package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketButtonConfigTest {
    @Test
    void missingFileUsesThirdRowDefaults() {
        MarketButtonConfig.Loaded loaded = MarketButtonConfig.parse(new MarketButtonConfig.FileData());
        assertEquals(MarketButtonConfig.DEFAULT_X, loaded.inventoryX());
        assertEquals(MarketButtonConfig.DEFAULT_Y, loaded.inventoryY());
        assertEquals(MarketButtonConfig.DEFAULT_X, loaded.creativeX());
        assertEquals(MarketButtonConfig.DEFAULT_Y, loaded.creativeY());
    }

    @Test
    void coordinatesClampToScreenRange() {
        MarketButtonConfig.FileData data = new MarketButtonConfig.FileData();
        data.inventoryX = -8;
        data.inventoryY = 4000;
        MarketButtonConfig.Loaded loaded = MarketButtonConfig.parse(data);
        assertEquals(0, loaded.inventoryX());
        assertEquals(1024, loaded.inventoryY());
    }
}
