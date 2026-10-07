package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferBuyPriceHistoryTest {
    @Test
    void recordsChronologicalSamples() {
        OfferBuyPriceHistory history = new OfferBuyPriceHistory(4);
        history.record(10L);
        history.record(12L);
        history.record(11L);
        assertArrayEquals(new long[] {10L, 12L, 11L}, history.snapshot());
        assertEquals(3, history.size());
    }

    @Test
    void dropsOldestWhenCapacityIsReached() {
        OfferBuyPriceHistory history = new OfferBuyPriceHistory(3);
        history.record(1L);
        history.record(2L);
        history.record(3L);
        history.record(4L);
        history.record(5L);
        assertArrayEquals(new long[] {3L, 4L, 5L}, history.snapshot());
        assertEquals(3, history.size());
        assertEquals(3, history.capacity());
    }

    @Test
    void ignoresNonPositivePrices() {
        OfferBuyPriceHistory history = new OfferBuyPriceHistory(8);
        history.record(0L);
        history.record(-4L);
        history.record(9L);
        assertArrayEquals(new long[] {9L}, history.snapshot());
    }

    @Test
    void fromSnapshotKeepsTheNewestSamplesWithinCapacity() {
        long[] source = {1L, 2L, 3L, 4L, 5L};
        OfferBuyPriceHistory history = OfferBuyPriceHistory.fromSnapshot(source, 3);
        assertArrayEquals(new long[] {3L, 4L, 5L}, history.snapshot());
    }

    @Test
    void defaultCapacityIsFortyEight() {
        assertEquals(48, OfferBuyPriceHistory.DEFAULT_CAPACITY);
        OfferBuyPriceHistory history = new OfferBuyPriceHistory(OfferBuyPriceHistory.DEFAULT_CAPACITY);
        for (int index = 1; index <= 50; index++) {
            history.record(index);
        }
        long[] snapshot = history.snapshot();
        assertEquals(48, snapshot.length);
        assertEquals(3L, snapshot[0]);
        assertEquals(50L, snapshot[47]);
    }
}
