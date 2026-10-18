package com.mawlee.cointcore.shop;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure gluon commission math.
 *
 * <p>Fee is computed once on the given amount (the deal total: {@code price × quantity}),
 * never per unit and then multiplied. Rounding is {@link RoundingMode#FLOOR} so the
 * effective rate never exceeds the configured percent: leftover fractions of a gluon
 * stay with the non-house party (seller-favor when the fee is taken from proceeds;
 * the buyer is not charged a larger rate than configured when the fee is added).
 */
public final class Commission {
    private Commission() {
    }

    /**
     * @param amount            deal total in whole gluons (negative treated as 0)
     * @param commissionPercent e.g. 2.5 for 2.5%; non-positive yields a zero fee
     */
    public static Result of(long amount, double commissionPercent) {
        long safeAmount = Math.max(0L, amount);
        if (safeAmount == 0L || commissionPercent <= 0.0d || Double.isNaN(commissionPercent)) {
            return new Result(0L, safeAmount);
        }
        if (Double.isInfinite(commissionPercent)) {
            return new Result(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        BigDecimal feeDecimal = BigDecimal.valueOf(safeAmount)
                .multiply(BigDecimal.valueOf(commissionPercent))
                .divide(BigDecimal.valueOf(100L), 0, RoundingMode.FLOOR);
        long fee = saturateToLong(feeDecimal);
        return new Result(fee, saturatingAdd(safeAmount, fee));
    }

    /**
     * Commission on {@code pricePerUnit × quantity} as a single amount.
     */
    public static Result ofDeal(long pricePerUnit, long quantity, double commissionPercent) {
        long price = Math.max(0L, pricePerUnit);
        long qty = quantity < 1L ? 0L : quantity;
        return of(saturatingMul(price, qty), commissionPercent);
    }

    /**
     * Buyer debit for a player/NPC buy: listing total plus the deal fee.
     */
    public static long buyerDebit(long pricePerUnit, long quantity, double commissionPercent) {
        return ofDeal(pricePerUnit, quantity, commissionPercent).total();
    }

    /**
     * Seller credit when the listing price is paid in full and the fee is burned
     * from the buyer's extra, or the net when selling (price − fee).
     */
    public static long sellerNet(long pricePerUnit, long quantity, double commissionPercent) {
        long gross = saturatingMul(Math.max(0L, pricePerUnit), quantity < 1L ? 0L : quantity);
        if (gross == 0L) {
            return 0L;
        }
        long fee = of(gross, commissionPercent).fee();
        if (fee >= gross) {
            return 0L;
        }
        return gross - fee;
    }

    /**
     * Largest {@code n} in {@code [0, available]} such that
     * {@link #buyerDebit(long, long, double) buyerDebit(price, n, percent)} ≤ {@code balance}.
     */
    public static long maxAffordableUnits(long pricePerUnit, double commissionPercent, long balance, long available) {
        long price = Math.max(0L, pricePerUnit);
        if (price < 1L || balance < 1L || available < 1L) {
            return 0L;
        }
        if (buyerDebit(price, 1L, commissionPercent) > balance) {
            return 0L;
        }
        long lo = 1L;
        long hi = available;
        while (lo < hi) {
            long mid = lo + (hi - lo + 1L) / 2L;
            long debit = buyerDebit(price, mid, commissionPercent);
            if (debit > 0L && debit <= balance) {
                lo = mid;
            } else {
                hi = mid - 1L;
            }
        }
        return lo;
    }

    static long saturatingMul(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
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
