package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalMarketPriceMathTest {
    @Test
    void itemKeyIgnoresCountAndJoinsComponents() {
        assertEquals("minecraft:bone_meal|", GlobalMarketPriceMath.itemKey("minecraft:bone_meal", ""));
        assertEquals("minecraft:bone_meal|{dmg}", GlobalMarketPriceMath.itemKey("minecraft:bone_meal", "{dmg}"));
    }

    @Test
    void unitPriceDividesDealPrice() {
        assertEquals(82L, GlobalMarketPriceMath.unitPrice(248L, 3));
        assertEquals(248L, GlobalMarketPriceMath.unitPrice(248L, 1));
        assertEquals(0L, GlobalMarketPriceMath.unitPrice(0L, 3));
        assertEquals(1L, GlobalMarketPriceMath.unitPrice(1L, 8));
        assertEquals(744L, GlobalMarketPriceMath.dealPrice(248L, 3));
    }

    @Test
    void medianOddTakesCenter() {
        assertEquals(100L, GlobalMarketPriceMath.median(List.of(80L, 100L, 120L)));
        assertEquals(50L, GlobalMarketPriceMath.median(List.of(50L)));
    }

    @Test
    void medianEvenAveragesMiddlePair() {
        assertEquals(90L, GlobalMarketPriceMath.median(List.of(80L, 100L)));
        assertEquals(15L, GlobalMarketPriceMath.median(List.of(10L, 12L, 18L, 40L)));
    }

    @Test
    void medianIgnoresEmptyAndNonPositive() {
        assertEquals(0L, GlobalMarketPriceMath.median(List.of()));
        assertEquals(0L, GlobalMarketPriceMath.median(null));
        assertEquals(7L, GlobalMarketPriceMath.median(List.of(0L, -2L, 7L)));
    }

    @Test
    void pruneKeepsWindowThenCapsCount() {
        long now = 30L * GlobalMarketMath.DAY_MS;
        List<GlobalMarketPriceMath.Sale> sales = List.of(
                new GlobalMarketPriceMath.Sale("a", 10L, now - 20L * GlobalMarketMath.DAY_MS),
                new GlobalMarketPriceMath.Sale("a", 20L, now - 5L * GlobalMarketMath.DAY_MS),
                new GlobalMarketPriceMath.Sale("a", 30L, now - 2L * GlobalMarketMath.DAY_MS),
                new GlobalMarketPriceMath.Sale("a", 40L, now - 1L * GlobalMarketMath.DAY_MS)
        );
        List<GlobalMarketPriceMath.Sale> window = GlobalMarketPriceMath.prune(sales, now, 14, 200);
        assertEquals(3, window.size());
        assertEquals(20L, window.getFirst().unitPrice());

        List<GlobalMarketPriceMath.Sale> capped = GlobalMarketPriceMath.prune(sales, now, 30, 2);
        assertEquals(2, capped.size());
        assertEquals(30L, capped.getFirst().unitPrice());
        assertEquals(40L, capped.getLast().unitPrice());
    }

    @Test
    void recommendUsesMedianThenListingFallback() {
        assertEquals(100L, GlobalMarketPriceMath.recommend(List.of(80L, 100L, 140L), 50L));
        assertEquals(50L, GlobalMarketPriceMath.recommend(List.of(), 50L));
        assertEquals(0L, GlobalMarketPriceMath.recommend(List.of(), 0L));
        assertTrue(GlobalMarketPriceMath.recommend(List.of(10L, 10L, 10L), 999L) == 10L);
    }
}
