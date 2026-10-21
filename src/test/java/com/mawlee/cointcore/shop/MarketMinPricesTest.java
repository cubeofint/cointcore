package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketMinPricesTest {
    private final MarketMinPrices prices = new MarketMinPrices(
            Map.of("minecraft:diamond", 50L),
            Map.of("c:gems", 20L, "c:gems/diamond", 80L)
    );

    @Test
    void highestMatchingRuleWins() {
        assertEquals(80L, prices.minUnitPrice("minecraft:diamond", Stream.of("c:gems", "c:gems/diamond")));
        assertEquals(50L, prices.minUnitPrice("minecraft:diamond", Stream.of()));
        assertEquals(20L, prices.minUnitPrice("minecraft:emerald", Stream.of("c:gems")));
        assertEquals(0L, prices.minUnitPrice("minecraft:dirt", Stream.of("minecraft:dirt")));
    }

    @Test
    void comparesPricePerItem() {
        assertFalse(MarketMinPrices.belowMinimum(500, 10, 50));
        assertTrue(MarketMinPrices.belowMinimum(499, 10, 50));
        assertFalse(MarketMinPrices.belowMinimum(1, 64, 0));
        assertTrue(MarketMinPrices.belowMinimum(Integer.MAX_VALUE, 64, Long.MAX_VALUE / 2));
    }
}
