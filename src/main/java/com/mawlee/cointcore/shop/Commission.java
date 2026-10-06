package com.mawlee.cointcore.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure gluon commission math. Fee is rounded up to a whole gluon.
 */
public final class Commission {
    private Commission() {
    }

    /**
     * @param price             item price in whole gluons (negative treated as 0)
     * @param commissionPercent e.g. 2.5 for 2.5%; non-positive yields a zero fee
     */
    public static Result of(long price, double commissionPercent) {
        long safePrice = Math.max(0L, price);
        if (safePrice == 0L || commissionPercent <= 0.0d || Double.isNaN(commissionPercent)) {
            return new Result(0L, safePrice);
        }
        if (Double.isInfinite(commissionPercent)) {
            return new Result(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        BigDecimal feeDecimal = BigDecimal.valueOf(safePrice)
                .multiply(BigDecimal.valueOf(commissionPercent))
                .divide(BigDecimal.valueOf(100L), 0, RoundingMode.CEILING);
        long fee = saturateToLong(feeDecimal);
        return new Result(fee, saturatingAdd(safePrice, fee));
    }

    private static long saturateToLong(BigDecimal value) {
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) >= 0) {
            return Long.MAX_VALUE;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        return value.longValue();
    }

    private static long saturatingAdd(long left, long right) {
        long sum = left + right;
        if (((left ^ sum) & (right ^ sum)) < 0L) {
            return Long.MAX_VALUE;
        }
        return sum;
    }

    public record Result(long fee, long total) {
    }
}
