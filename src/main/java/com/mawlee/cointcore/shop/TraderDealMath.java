package com.mawlee.cointcore.shop;

/**
 * Pure quantity / money helpers for trader deals. One unit is one offer listing
 * ({@code offer.count()} items at the listing's buy/sell totals).
 */
public final class TraderDealMath {
    private TraderDealMath() {
    }

    /**
     * How many listing units fit in one max-size stack of the offered item.
     */
    public static int unitsPerStack(int offerCount, int maxStackSize) {
        int count = Math.max(1, offerCount);
        int max = Math.max(1, maxStackSize);
        return Math.max(1, max / count);
    }

    /**
     * Units that {@code matchingItems} can fill, given listing size {@code offerCount}.
     */
    public static int itemUnits(int matchingOrSpaceItems, int offerCount) {
        int count = Math.max(1, offerCount);
        if (matchingOrSpaceItems <= 0) {
            return 0;
        }
        return matchingOrSpaceItems / count;
    }

    /**
     * Units the wallet can pay for, capped by {@code maxUnits}.
     */
    public static int affordableUnits(long unitCost, long balance, int maxUnits) {
        if (unitCost <= 0L || balance < unitCost || maxUnits <= 0) {
            return 0;
        }
        long byMoney = balance / unitCost;
        return (int) Math.min(maxUnits, byMoney);
    }

    public static int resolveUnits(int requested, int byItems, int byMoney) {
        int wanted = Math.max(1, requested);
        return Math.min(wanted, Math.min(Math.max(0, byItems), Math.max(0, byMoney)));
    }

    public static long cost(long unitAmount, int units) {
        if (units <= 0 || unitAmount <= 0L) {
            return 0L;
        }
        return saturatingMul(unitAmount, units);
    }

    static long saturatingMul(long left, int right) {
        if (right <= 0 || left <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}
