package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketButtonLayoutTest {
    @Test
    void missingFileUsesThirdRowDefaults() {
        MarketButtonLayout.Coords loaded = MarketButtonLayout.parse(null, null, null, null);
        assertEquals(MarketButtonLayout.DEFAULT_X, loaded.inventoryX());
        assertEquals(MarketButtonLayout.DEFAULT_Y, loaded.inventoryY());
        assertEquals(MarketButtonLayout.DEFAULT_X, loaded.creativeX());
        assertEquals(MarketButtonLayout.DEFAULT_Y, loaded.creativeY());
    }

    @Test
    void coordinatesClampToScreenRange() {
        MarketButtonLayout.Coords loaded = MarketButtonLayout.parse(-8, 4000, null, null);
        assertEquals(0, loaded.inventoryX());
        assertEquals(1024, loaded.inventoryY());
    }
}
