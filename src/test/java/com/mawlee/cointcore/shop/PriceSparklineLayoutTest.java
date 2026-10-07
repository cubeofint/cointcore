package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceSparklineLayoutTest {
    @Test
    void highestPriceMapsToTop() {
        int[] ys = PriceSparklineLayout.toY(new long[] {10L, 20L, 30L}, 13);
        assertEquals(12, ys[0]);
        assertEquals(0, ys[2]);
    }

    @Test
    void flatSeriesSitsOnTheMidline() {
        int[] ys = PriceSparklineLayout.toY(new long[] {7L, 7L, 7L}, 13);
        assertArrayEquals(new int[] {6, 6, 6}, ys);
    }

    @Test
    void xSpreadsAcrossInnerWidth() {
        assertEquals(0, PriceSparklineLayout.xForIndex(0, 5, 38));
        assertEquals(37, PriceSparklineLayout.xForIndex(4, 5, 38));
        assertEquals(19, PriceSparklineLayout.xForIndex(0, 1, 38));
    }
}
