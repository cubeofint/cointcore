package com.mawlee.cointcore.relics;

/**
 * Decides whether Relics may walk Sophisticated Backpack nested inventories this tick.
 */
public final class RelicsBackpackScanPolicy {
    public enum Mode {
        SKIP_NESTED,
        THROTTLE
    }

    private RelicsBackpackScanPolicy() {
    }

    /**
     * Pure variant for unit tests / callers that already resolved config.
     */
    public static boolean shouldScanNested(boolean enabled, Mode mode, int intervalTicks, long gameTime) {
        if (!enabled) {
            return true;
        }
        return switch (mode) {
            case SKIP_NESTED -> false;
            case THROTTLE -> intervalTicks <= 1 || Math.floorMod(gameTime, intervalTicks) == 0;
        };
    }
}
