package com.mawlee.cointcore.rftools;

import com.mawlee.cointcore.config.MachinePerfConfig;
import net.minecraft.world.level.Level;

/**
 * Shared per-server-tick budgets for all RFTools Builders.
 * Resets when {@link Level#getGameTime()} advances.
 */
public final class RfToolsBuilderBudget {
    private static long trackedGameTime = Long.MIN_VALUE;
    private static int usedBlocks;
    private static int usedFormulaRebuilds;
    private static int usedChunkLoads;

    private RfToolsBuilderBudget() {
    }

    /**
     * @return {@code true} if one builder block-op may run; {@code false} if the global cap is exhausted
     */
    public static boolean tryConsume(Level level) {
        return consume(level, Kind.BLOCK);
    }

    /**
     * One shape-formula rebuild ({@code composeFormula}) per server tick by default.
     * A denied rebuild must not advance the quarry cursor.
     */
    public static boolean tryConsumeFormulaRebuild(Level level) {
        return consume(level, Kind.FORMULA);
    }

    /**
     * One new quarry chunk ticket per server tick by default.
     */
    public static boolean tryConsumeChunkLoad(Level level) {
        return consume(level, Kind.CHUNK);
    }

    /** Gives back one block slot reserved for work that did not break or place a block. */
    public static void refundBlock(Level level) {
        if (!counts(level) || limit(Kind.BLOCK) <= 0) {
            return;
        }
        roll(level);
        if (usedBlocks > 0) {
            usedBlocks--;
        }
    }

    private static boolean consume(Level level, Kind kind) {
        if (!counts(level)) {
            return true;
        }
        int cap = limit(kind);
        if (cap <= 0) {
            return true;
        }
        roll(level);
        int used = used(kind);
        if (used >= cap) {
            return false;
        }
        setUsed(kind, used + 1);
        return true;
    }

    private static boolean counts(Level level) {
        return level != null && !level.isClientSide() && MachinePerfConfig.isRfToolsBuilderEnabled();
    }

    private static void roll(Level level) {
        long gameTime = level.getGameTime();
        if (gameTime == trackedGameTime) {
            return;
        }
        trackedGameTime = gameTime;
        usedBlocks = 0;
        usedFormulaRebuilds = 0;
        usedChunkLoads = 0;
    }

    private static int limit(Kind kind) {
        return switch (kind) {
            case BLOCK -> MachinePerfConfig.getRfToolsGlobalBlocksPerTick();
            case FORMULA -> MachinePerfConfig.getRfToolsFormulaRebuildsPerTick();
            case CHUNK -> MachinePerfConfig.getRfToolsChunkLoadsPerTick();
        };
    }

    private static int used(Kind kind) {
        return switch (kind) {
            case BLOCK -> usedBlocks;
            case FORMULA -> usedFormulaRebuilds;
            case CHUNK -> usedChunkLoads;
        };
    }

    private static void setUsed(Kind kind, int value) {
        switch (kind) {
            case BLOCK -> usedBlocks = value;
            case FORMULA -> usedFormulaRebuilds = value;
            case CHUNK -> usedChunkLoads = value;
        }
    }

    private enum Kind {
        BLOCK,
        FORMULA,
        CHUNK
    }
}
