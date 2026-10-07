package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferPriceVerdictTest {
    @Test
    void fivePercentBandAroundAverageIsFair() {
        assertEquals(OfferPriceVerdict.FAIR, OfferPriceVerdict.classify(100L, 100L, 5.0d));
        assertEquals(OfferPriceVerdict.FAIR, OfferPriceVerdict.classify(95L, 100L, 5.0d));
        assertEquals(OfferPriceVerdict.FAIR, OfferPriceVerdict.classify(105L, 100L, 5.0d));
    }

    @Test
    void belowBandIsCheap() {
        assertEquals(OfferPriceVerdict.CHEAP, OfferPriceVerdict.classify(94L, 100L, 5.0d));
        assertEquals(OfferPriceVerdict.CHEAP, OfferPriceVerdict.classify(88L, 100L, 5.0d));
    }

    @Test
    void aboveBandIsExpensive() {
        assertEquals(OfferPriceVerdict.EXPENSIVE, OfferPriceVerdict.classify(106L, 100L, 5.0d));
        assertEquals(OfferPriceVerdict.EXPENSIVE, OfferPriceVerdict.classify(112L, 100L, 5.0d));
    }

    @Test
    void missingPricesAreUnknown() {
        assertEquals(OfferPriceVerdict.UNKNOWN, OfferPriceVerdict.classify(0L, 100L, 5.0d));
        assertEquals(OfferPriceVerdict.UNKNOWN, OfferPriceVerdict.classify(100L, 0L, 5.0d));
    }

    @Test
    void percentGapMatchesTooltipCopy() {
        assertEquals(12, OfferPriceVerdict.percentGap(88L, 100L));
        assertEquals(8, OfferPriceVerdict.percentGap(108L, 100L));
        assertEquals(0, OfferPriceVerdict.percentGap(100L, 100L));
        assertEquals(0, OfferPriceVerdict.percentGap(50L, 0L));
    }

    @Test
    void statsUseCurrentBuyAndHistoryAverage() {
        OfferPriceStats stats = OfferPriceStats.of(new long[] {80L, 100L, 120L}, 88L);
        assertEquals(80L, stats.min());
        assertEquals(120L, stats.max());
        assertEquals(100L, stats.average());
        assertEquals(88L, stats.current());
        assertEquals(3, stats.count());
        assertEquals(OfferPriceVerdict.CHEAP, OfferPriceVerdict.classify(stats.current(), stats.average(), 5.0d));
        assertEquals(12, OfferPriceVerdict.percentGap(stats.current(), stats.average()));
    }

    @Test
    void emptyHistoryHasZeroCount() {
        OfferPriceStats stats = OfferPriceStats.of(new long[0], 40L);
        assertEquals(0, stats.count());
        assertEquals(40L, stats.current());
        assertEquals(OfferPriceVerdict.UNKNOWN, OfferPriceVerdict.classify(stats.current(), stats.average(), 5.0d));
    }
}
