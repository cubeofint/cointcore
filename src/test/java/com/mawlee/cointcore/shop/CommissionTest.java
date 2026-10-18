package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionTest {
    @Test
    void zeroPercentHasNoFee() {
        Commission.Result result = Commission.of(1000L, 0.0d);
        assertEquals(0L, result.fee());
        assertEquals(1000L, result.total());
    }

    @Test
    void wholePercent() {
        Commission.Result result = Commission.of(100L, 10.0d);
        assertEquals(10L, result.fee());
        assertEquals(110L, result.total());
    }

    @Test
    void fractionalFeeFloorsAndNeverExceedsRate() {
        Commission.Result onePercentOfOne = Commission.of(1L, 1.0d);
        assertEquals(0L, onePercentOfOne.fee());
        assertEquals(1L, onePercentOfOne.total());

        Commission.Result twoPointFiveOfOne = Commission.of(1L, 2.5d);
        assertEquals(0L, twoPointFiveOfOne.fee());
        assertEquals(1L, twoPointFiveOfOne.total());

        Commission.Result twoPointFiveOfThirtyNine = Commission.of(39L, 2.5d);
        assertEquals(0L, twoPointFiveOfThirtyNine.fee());
        assertEquals(39L, twoPointFiveOfThirtyNine.total());
    }

    @Test
    void exactFractionalPercentDoesNotOvercharge() {
        Commission.Result result = Commission.of(40L, 2.5d);
        assertEquals(1L, result.fee());
        assertEquals(41L, result.total());
    }

    @Test
    void cheapItemsTimesQuantityUsesOneFeeOnDealTotal() {
        Commission.Result oneGluonStack = Commission.ofDeal(1L, 1024L, 2.5d);
        assertEquals(25L, oneGluonStack.fee());
        assertEquals(1049L, oneGluonStack.total());
        assertEquals(1024L, GlobalMarketMath.sellerCredit(1L, 1024L));
        assertEquals(oneGluonStack.total(), 1024L + oneGluonStack.fee());
        assertEquals(999L, Commission.sellerNet(1L, 1024L, 2.5d));

        Commission.Result twoGluons = Commission.ofDeal(2L, 64L, 2.5d);
        assertEquals(3L, twoGluons.fee());
        assertEquals(131L, twoGluons.total());

        Commission.Result threeGluons = Commission.ofDeal(3L, 64L, 2.5d);
        assertEquals(4L, threeGluons.fee());
        assertEquals(196L, threeGluons.total());
    }

    @Test
    void reportedBugExamplesStayAtConfiguredRate() {
        Commission.Result buy2048Units = Commission.ofDeal(1L, 1024L, 2.5d);
        assertEquals(1049L, buy2048Units.total());
        assertEquals(1024L, GlobalMarketMath.sellerCredit(1L, 1024L));
        assertEquals(buy2048Units.total(), 1024L + buy2048Units.fee());
        assertTrue(buy2048Units.fee() * 100.0d / 1024.0d <= 2.5d);

        Commission.Result sell396 = Commission.ofDeal(6L, 66L, 2.5d);
        assertEquals(9L, sell396.fee());
        assertEquals(387L, Commission.sellerNet(6L, 66L, 2.5d));
        assertEquals(396L, 387L + sell396.fee());
    }

    @Test
    void largeQuantityAndRateEdges() {
        assertEquals(new Commission.Result(25_000L, 1_025_000L), Commission.ofDeal(1L, 1_000_000L, 2.5d));
        assertEquals(0L, Commission.ofDeal(1L, 1L, 0.0d).fee());
        assertEquals(0L, Commission.ofDeal(1L, 1L, 2.5d).fee());
        assertEquals(50L, Commission.of(50L, 100.0d).fee());
        assertEquals(100L, Commission.of(50L, 200.0d).fee());
        assertEquals(0L, Commission.sellerNet(50L, 1L, 200.0d));
        assertEquals(64L, Commission.maxAffordableUnits(1L, 2.5d, 65L, 64L));
        assertEquals(63L, Commission.maxAffordableUnits(1L, 2.5d, 64L, 64L));
    }

    @Test
    void zeroPrice() {
        Commission.Result result = Commission.of(0L, 15.0d);
        assertEquals(0L, result.fee());
        assertEquals(0L, result.total());
    }

    @Test
    void negativePriceAndPercentAreClamped() {
        assertEquals(new Commission.Result(0L, 0L), Commission.of(-50L, 10.0d));
        assertEquals(new Commission.Result(0L, 80L), Commission.of(80L, -3.0d));
        assertEquals(new Commission.Result(0L, 80L), Commission.of(80L, Double.NaN));
    }

    @Test
    void fullPercent() {
        Commission.Result result = Commission.of(50L, 100.0d);
        assertEquals(50L, result.fee());
        assertEquals(100L, result.total());
    }

    @Test
    void saturatesOnOverflow() {
        Commission.Result result = Commission.of(Long.MAX_VALUE, 1.0d);
        assertEquals(92_233_720_368_547_758L, result.fee());
        assertEquals(Long.MAX_VALUE, result.total());

        Commission.Result hugePercent = Commission.of(Long.MAX_VALUE, 200.0d);
        assertEquals(Long.MAX_VALUE, hugePercent.fee());
        assertEquals(Long.MAX_VALUE, hugePercent.total());
    }
}
