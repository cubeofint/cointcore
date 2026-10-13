package com.mawlee.cointcore.watchdog;

/**
 * Pure sampling policy for per-BE/entity timing. Whole-tick nanoTime stays always-on.
 */
public final class WatchdogSampling {
    private WatchdogSampling() {
    }

    public static boolean isArmed(boolean detailedAlways, boolean detailedOff, int holdRemaining) {
        if (detailedOff) {
            return false;
        }
        if (detailedAlways) {
            return true;
        }
        return holdRemaining > 0;
    }

    public static boolean shouldSampleDetails(
            boolean armed,
            boolean forceSample,
            int tickIndex,
            int sampleEveryNTicks
    ) {
        if (!armed) {
            return false;
        }
        if (forceSample) {
            return true;
        }
        int n = Math.max(1, sampleEveryNTicks);
        return n == 1 || Math.floorMod(tickIndex, n) == 0;
    }
}
