package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-tick exclusive claim of a Soul Surge target. One surge may accelerate a
 * given cell each game tick; the surge that held the claim last tick keeps it
 * (sticky owner) so souls are not drained in alternation.
 * <p>
 * O(1) per {@link #tryClaim} / {@link #release}: no world scans. A surge
 * that cannot accelerate must {@link #release} immediately (no souls,
 * removed, unloaded, retargeted). Otherwise a stale owner drops out after
 * one idle tick.
 */
public final class SoulSurgeAccelerationClaim {
    private static final int PACKED_XZ_LENGTH = 26;
    private static final int PACKED_Y_LENGTH = 12;
    private static final long XZ_MASK = (1L << PACKED_XZ_LENGTH) - 1L;
    private static final long Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
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
     * Current-tick owner, or the sticky owner from the previous tick.
     */
    public Long peekOwner(long targetPacked, long gameTime) {
        roll(gameTime);
        Long current = currentOwners.get(targetPacked);
        if (current != null) {
            return current;
        }
        return lastOwners.get(targetPacked);
    }

    /**
     * Owner that already claimed this tick. {@code null} if only a sticky
     * owner from the previous tick remains (not yet active this tick).
     */
    public Long peekCurrentOwner(long targetPacked, long gameTime) {
        roll(gameTime);
        return currentOwners.get(targetPacked);
    }

    public boolean isOwnerActive(long targetPacked, long surgePacked, long gameTime) {
        Long current = peekCurrentOwner(targetPacked, gameTime);
        return current != null && current == surgePacked;
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
