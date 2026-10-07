package com.mawlee.cointcore.shop;

/**
 * Current buy price versus the history average. Band is a percent of the average (default 5).
 */
public enum OfferPriceVerdict {
    CHEAP,
    FAIR,
    EXPENSIVE,
    UNKNOWN;

    public static OfferPriceVerdict classify(long current, long average, double bandPercent) {
        if (current <= 0L || average <= 0L) {
            return UNKNOWN;
        }
        long allowed = Math.max(0L, Math.round(average * (Math.max(0.0d, bandPercent) / 100.0d)));
        if (current < average - allowed) {
            return CHEAP;
        }
        if (current > average + allowed) {
            return EXPENSIVE;
        }
        return FAIR;
    }

    /**
     * Absolute percent gap versus average, rounded to nearest integer.
     * Cheap: how much below average; expensive: how much above.
     */
    public static int percentGap(long current, long average) {
        if (average <= 0L) {
            return 0;
        }
        return (int) Math.round(Math.abs(current - average) * 100.0d / average);
    }
}
