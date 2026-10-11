package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-tick exclusive claim of a Soul Surge target. One surge may accelerate a
 * given cell each game tick; the surge that held the claim last tick keeps it
 * (sticky owner) so souls are not drained in alternation.
 * <p>
 * O(1) per {@link #tryClaim} / {@link #release}: no world scans. Stale owners
 * drop out when they stop claiming — after one idle tick, or immediately if
 * they {@link #release}.
 */
public final class SoulSurgeAccelerationClaim {
    private static final long XZ_MASK = (1L << 26) - 1L;
    private static final long Y_MASK = (1L << 12) - 1L;
    private static final int Z_OFFSET = 12;
    private static final int X_OFFSET = 38;

    private long currentTick = Long.MIN_VALUE;
    private Map<Long, Long> lastOwners = new HashMap<>();
    private Map<Long, Long> currentOwners = new HashMap<>();

    /**
     * Packs a block position with the same layout as {@code BlockPos.asLong()}.
     */
    public static long pack(int x, int y, int z) {
        return (x & XZ_MASK) << X_OFFSET | (z & XZ_MASK) << Z_OFFSET | (y & Y_MASK);
    }

    /**
     * @return {@code true} if {@code surgePacked} may accelerate {@code targetPacked}
     * this tick (first claimant, or the sticky owner from last tick)
     */
    public boolean tryClaim(long targetPacked, long surgePacked, long gameTime) {
        roll(gameTime);
        Long current = currentOwners.get(targetPacked);
        if (current != null) {
            return current == surgePacked;
        }
        Long last = lastOwners.get(targetPacked);
        if (last != null && last != surgePacked) {
            return false;
        }
        currentOwners.put(targetPacked, surgePacked);
        return true;
    }

    /**
     * Drops the claim if {@code surgePacked} is the current or sticky owner so
     * another surge can take over the same tick.
     */
    public void release(long targetPacked, long surgePacked, long gameTime) {
        roll(gameTime);
        removeIfOwner(currentOwners, targetPacked, surgePacked);
        removeIfOwner(lastOwners, targetPacked, surgePacked);
    }

    private void roll(long gameTime) {
        if (gameTime == currentTick) {
            return;
        }
        if (gameTime == currentTick + 1L) {
            Map<Long, Long> idle = lastOwners;
            lastOwners = currentOwners;
            currentOwners = idle;
            currentOwners.clear();
        } else {
            lastOwners.clear();
            currentOwners.clear();
        }
        currentTick = gameTime;
    }

    private static void removeIfOwner(Map<Long, Long> owners, long targetPacked, long surgePacked) {
        Long owner = owners.get(targetPacked);
        if (owner != null && owner == surgePacked) {
            owners.remove(targetPacked);
        }
    }
}
