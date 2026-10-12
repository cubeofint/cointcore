package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-target extra-tick budget for Soul Surge. A machine may receive at most
 * {@link #MAX_EXTRA_TICKS} extra ticks in one game tick (4x including the
 * normal vanilla tick), no matter how many surges point at it.
 * <p>
 * O(1) per {@link #tryApplyExtraTick}: no world scans, no owner/claim.
 * The map is cleared whenever {@code gameTime} changes so idle positions
 * do not leak.
 */
public final class SoulSurgeAccelerationBudget {
    /**
     * Extra ticks on top of the normal block-entity tick. {@code 3} extra
     * is 4x total, matching the previous single-surge x4 cap.
     */
    public static final int MAX_EXTRA_TICKS = 3;

    private static final int PACKED_XZ_LENGTH = 26;
    private static final int PACKED_Y_LENGTH = 12;
    private static final long XZ_MASK = (1L << PACKED_XZ_LENGTH) - 1L;
    private static final long Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final int Z_OFFSET = 12;
    private static final int X_OFFSET = 38;

    private long currentTick = Long.MIN_VALUE;
    private final Map<Long, Integer> extraTicks = new HashMap<>();

    /**
     * Packs a block position with the same layout as {@code BlockPos.asLong()}.
     */
    public static long pack(int x, int y, int z) {
        return (x & XZ_MASK) << X_OFFSET | (z & XZ_MASK) << Z_OFFSET | (y & Y_MASK);
    }

    public static int unpackX(long packed) {
        return (int) (packed << (64 - X_OFFSET - PACKED_XZ_LENGTH) >> (64 - PACKED_XZ_LENGTH));
    }

    public static int unpackY(long packed) {
        return (int) (packed << (64 - PACKED_Y_LENGTH) >> (64 - PACKED_Y_LENGTH));
    }

    public static int unpackZ(long packed) {
        return (int) (packed << (64 - Z_OFFSET - PACKED_XZ_LENGTH) >> (64 - PACKED_XZ_LENGTH));
    }

    /**
     * Extra ticks already applied to {@code targetPacked} this game tick.
     */
    public int extraTicksApplied(long targetPacked, long gameTime) {
        roll(gameTime);
        return extraTicks.getOrDefault(targetPacked, 0);
    }

    /**
     * {@code true} if another extra tick may still be applied this game tick.
     */
    public boolean hasRemaining(long targetPacked, long gameTime) {
        return extraTicksApplied(targetPacked, gameTime) < MAX_EXTRA_TICKS;
    }

    /**
     * Applies one extra tick if the target is still under the 4x cap.
     *
     * @return {@code true} if the caller should invoke the block-entity ticker
     */
    public boolean tryApplyExtraTick(long targetPacked, long gameTime) {
        roll(gameTime);
        int used = extraTicks.getOrDefault(targetPacked, 0);
        if (used >= MAX_EXTRA_TICKS) {
            return false;
        }
        extraTicks.put(targetPacked, used + 1);
        return true;
    }

    private void roll(long gameTime) {
        if (gameTime == currentTick) {
            return;
        }
        extraTicks.clear();
        currentTick = gameTime;
    }
}
