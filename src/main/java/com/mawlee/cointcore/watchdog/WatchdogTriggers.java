package com.mawlee.cointcore.watchdog;

/**
 * Pure trigger decisions for the tick watchdog.
 */
public final class WatchdogTriggers {
    private WatchdogTriggers() {
    }

    public static boolean consecutiveSlowTicks(boolean lastTickSlow, int consecutiveSlow, int required) {
        return lastTickSlow && consecutiveSlow >= Math.max(1, required);
    }

    public static boolean sustainedAverage(
            double averageMs,
            double thresholdMs,
            int samples,
            int requiredSamples
    ) {
        return requiredSamples > 0
                && samples >= requiredSamples
                && averageMs > thresholdMs;
    }
}
