package com.mawlee.cointcore.industrialforegoingsouls;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-level sticky claim map used by the Soul Surge mixin and the admin debug
 * command. One surge may accelerate a given cell each game tick.
 */
public final class SoulSurgeClaimTracker {
    private static final Map<Level, SoulSurgeAccelerationClaim> BY_LEVEL = new WeakHashMap<>();

    private SoulSurgeClaimTracker() {
    }

    public static SoulSurgeAccelerationClaim forLevel(Level level) {
        return BY_LEVEL.computeIfAbsent(level, ignored -> new SoulSurgeAccelerationClaim());
    }

    /**
     * Packed owner of {@code targetPos} this tick (or the sticky owner from the
     * previous tick). {@code null} if nobody holds the claim.
     */
    public static Long peekOwnerPacked(Level level, BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return null;
        }
        return forLevel(level).peekOwner(
                SoulSurgeAccelerationClaim.pack(targetPos.getX(), targetPos.getY(), targetPos.getZ()),
                level.getGameTime()
        );
    }
}
