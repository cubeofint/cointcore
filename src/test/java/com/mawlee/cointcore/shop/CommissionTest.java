package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void fractionalFeeRoundsUp() {
        Commission.Result onePercentOfOne = Commission.of(1L, 1.0d);
        assertEquals(1L, onePercentOfOne.fee());
        assertEquals(2L, onePercentOfOne.total());

        Commission.Result twoPointFiveOfOne = Commission.of(1L, 2.5d);
        assertEquals(1L, twoPointFiveOfOne.fee());
        assertEquals(2L, twoPointFiveOfOne.total());

        Commission.Result twoPointFiveOfThirtyNine = Commission.of(39L, 2.5d);
        assertEquals(1L, twoPointFiveOfThirtyNine.fee());
        assertEquals(40L, twoPointFiveOfThirtyNine.total());
    }

    @Test
    void exactFractionalPercentDoesNotOvercharge() {
        Commission.Result result = Commission.of(40L, 2.5d);
        assertEquals(1L, result.fee());
        assertEquals(41L, result.total());
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
        assertEquals(92_233_720_368_547_759L, result.fee());
        assertEquals(Long.MAX_VALUE, result.total());

        Commission.Result hugePercent = Commission.of(Long.MAX_VALUE, 200.0d);
        assertEquals(Long.MAX_VALUE, hugePercent.fee());
        assertEquals(Long.MAX_VALUE, hugePercent.total());
    }
}
