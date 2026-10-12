package com.mawlee.cointcore.industrialforegoingsouls;

/**
 * Extra-tick cap for Soul Surge acceleration.
 * <p>
 * A target block entity may be accelerated at most 4x per game tick
 * (1 normal tick + {@link SoulSurgeAccelerationBudget#MAX_EXTRA_TICKS}
 * extra), regardless of how many surges point at it. Soul consumption
 * stays in Industrial Foregoing Souls: this only admits extra ticker
 * calls while the per-target counter is still under the cap.
 */
public final class SoulSurgeAcceleration {
    public static final int MAX_EXTRA_TICKS = SoulSurgeAccelerationBudget.MAX_EXTRA_TICKS;

    private SoulSurgeAcceleration() {
    }

    /**
     * Applies one extra tick to {@code targetPacked} if the cap still has
     * room this {@code gameTime}.
     */
    public static boolean tryApplyExtraTick(
            SoulSurgeAccelerationBudget budget,
            long targetPacked,
            long gameTime
    ) {
        return budget.tryApplyExtraTick(targetPacked, gameTime);
    }
}
