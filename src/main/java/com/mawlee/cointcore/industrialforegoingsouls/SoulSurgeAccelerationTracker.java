package com.mawlee.cointcore.industrialforegoingsouls;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-level extra-tick budget used by the Soul Surge mixin and the admin
 * debug command. Keyed by level + {@link BlockPos}; each level's map is
 * cleared on {@code gameTime} change.
 */
public final class SoulSurgeAccelerationTracker {
    private static final Map<Level, SoulSurgeAccelerationBudget> BY_LEVEL = new WeakHashMap<>();

    private SoulSurgeAccelerationTracker() {
    }

    public static SoulSurgeAccelerationBudget forLevel(Level level) {
        return BY_LEVEL.computeIfAbsent(level, ignored -> new SoulSurgeAccelerationBudget());
    }

    public static boolean tryApplyExtraTick(Level level, BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return false;
        }
        return forLevel(level).tryApplyExtraTick(pack(targetPos), level.getGameTime());
    }

    public static boolean hasRemaining(Level level, BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return false;
        }
        return forLevel(level).hasRemaining(pack(targetPos), level.getGameTime());
    }

    public static int extraTicksApplied(Level level, BlockPos targetPos) {
        if (level == null || targetPos == null) {
            return 0;
        }
        return forLevel(level).extraTicksApplied(pack(targetPos), level.getGameTime());
    }

    private static long pack(BlockPos pos) {
        return SoulSurgeAccelerationBudget.pack(pos.getX(), pos.getY(), pos.getZ());
    }
}
