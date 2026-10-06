package com.mawlee.cointcore.environment;

import com.mawlee.cointcore.config.VoteConfig;

/**
 * Shared cooldown for forced day-time / weather changes (votes, commands, mods).
 * Natural daylight/weather ticks and sleep skip are marked separately and bypass this.
 */
public final class TimeWeatherCooldown {
    private static final ThreadLocal<Integer> NATURAL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> FORCED_BATCH_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> FORCED_BATCH_DIRTY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> PASS_THROUGH_DEPTH = ThreadLocal.withInitial(() -> 0);

    /** Cross-tick forced ops (e.g. DE celestial time warp). */
    private static int sustainedForcedCount;

    /**
     * DE {@code TileCelestialManipulator} embeds {@code ClientLevel} in common bytecode, so we must
     * not mixin that class on dedicated servers. Time-warp is detected via {@code setDayTime} call
     * stacks and kept open until the warp goes idle.
     */
    private static final String CELESTIAL_MANIPULATOR = "TileCelestialManipulator";
    private static final long CELESTIAL_WARP_IDLE_MS = 3000L;
    private static boolean celestialWarpActive;
    private static long celestialWarpLastSeenMs;

    private static long cooldownUntilMs;

    private TimeWeatherCooldown() {
    }

    public static void clearRuntimeState() {
        cooldownUntilMs = 0L;
        sustainedForcedCount = 0;
        celestialWarpActive = false;
        celestialWarpLastSeenMs = 0L;
        NATURAL_DEPTH.remove();
        FORCED_BATCH_DEPTH.remove();
        FORCED_BATCH_DIRTY.remove();
        PASS_THROUGH_DEPTH.remove();
    }

    public static void tickCelestialWarpWatchdog() {
        if (!celestialWarpActive) {
            return;
        }
        if (System.currentTimeMillis() - celestialWarpLastSeenMs >= CELESTIAL_WARP_IDLE_MS) {
            celestialWarpActive = false;
        }
    }

    /**
     * Allows multi-tick DE celestial time/weather changes without mixin'ing
     * {@code TileCelestialManipulator} (that class embeds {@code ClientLevel} in common bytecode).
     */
    public static boolean tryAllowCelestialForcedEnvironment() {
        if (!VoteConfig.isEnvironmentCooldownEnabled()) {
            celestialWarpActive = true;
            celestialWarpLastSeenMs = System.currentTimeMillis();
            return true;
        }
        long now = System.currentTimeMillis();
        if (celestialWarpActive) {
            celestialWarpLastSeenMs = now;
            return true;
        }
        if (isOnCooldown()) {
            return false;
        }
        armCooldown();
        celestialWarpActive = true;
        celestialWarpLastSeenMs = now;
        return true;
    }

    public static boolean isCelestialManipulatorCaller() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().endsWith(CELESTIAL_MANIPULATOR)
                    || frame.getClassName().contains("." + CELESTIAL_MANIPULATOR + "$")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vanilla {@code /time} and {@code /weather} (ops / console). Must bypass the vote/mod
     * environment cooldown — otherwise Brigadier still prints success while {@code setDayTime}
     * is cancelled and nothing changes in-game.
     */
    public static boolean isVanillaTimeOrWeatherCommandCaller() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String className = frame.getClassName();
            if (className.equals("net.minecraft.server.commands.TimeCommand")
                    || className.equals("net.minecraft.server.commands.WeatherCommand")) {
                return true;
            }
        }
        return false;
    }

    public static void enterNatural() {
        NATURAL_DEPTH.set(NATURAL_DEPTH.get() + 1);
    }

    public static void exitNatural() {
        int depth = NATURAL_DEPTH.get() - 1;
        if (depth <= 0) {
            NATURAL_DEPTH.remove();
        } else {
            NATURAL_DEPTH.set(depth);
        }
    }

    public static boolean isNatural() {
        return NATURAL_DEPTH.get() > 0;
    }

    public static boolean isForcedBatch() {
        return FORCED_BATCH_DEPTH.get() > 0;
    }

    public static boolean isSustainedForced() {
        return sustainedForcedCount > 0;
    }

    /** Nested helpers after an entry point already consumed/armed the shared cooldown. */
    public static void enterPassThrough() {
        PASS_THROUGH_DEPTH.set(PASS_THROUGH_DEPTH.get() + 1);
    }

    public static void exitPassThrough() {
        int depth = PASS_THROUGH_DEPTH.get() - 1;
        if (depth <= 0) {
            PASS_THROUGH_DEPTH.remove();
        } else {
            PASS_THROUGH_DEPTH.set(depth);
        }
    }

    public static boolean isPassThrough() {
        return PASS_THROUGH_DEPTH.get() > 0;
    }

    public static int remainingSeconds() {
        long remaining = cooldownUntilMs - System.currentTimeMillis();
        if (remaining <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0D);
    }

    public static boolean isOnCooldown() {
        return remainingSeconds() > 0;
    }

    /**
     * Starts a multi-tick forced change. Arms shared cooldown once.
     * Subsequent {@link #tryAllowForcedChange()} calls succeed while sustained.
     */
    public static boolean tryBeginSustainedForced() {
        if (!VoteConfig.isEnvironmentCooldownEnabled()) {
            sustainedForcedCount++;
            return true;
        }
        if (sustainedForcedCount > 0) {
            sustainedForcedCount++;
            return true;
        }
        if (isOnCooldown()) {
            return false;
        }
        armCooldown();
        sustainedForcedCount = 1;
        return true;
    }

    public static void endSustainedForced() {
        if (sustainedForcedCount > 0) {
            sustainedForcedCount--;
        }
    }

    /**
     * Runs a batch of forced environment changes as one cooldown event.
     * Returns false if currently on cooldown (runnable is not executed).
     */
    public static boolean runForced(Runnable action) {
        if (!VoteConfig.isEnvironmentCooldownEnabled()) {
            action.run();
            return true;
        }
        if (isOnCooldown() && !isSustainedForced()) {
            return false;
        }
        int depth = FORCED_BATCH_DEPTH.get() + 1;
        FORCED_BATCH_DEPTH.set(depth);
        if (depth == 1) {
            FORCED_BATCH_DIRTY.set(false);
        }
        try {
            action.run();
        } finally {
            boolean dirty = Boolean.TRUE.equals(FORCED_BATCH_DIRTY.get());
            int nextDepth = FORCED_BATCH_DEPTH.get() - 1;
            if (nextDepth <= 0) {
                FORCED_BATCH_DEPTH.remove();
                FORCED_BATCH_DIRTY.remove();
                if (dirty && !isSustainedForced()) {
                    armCooldown();
                }
            } else {
                FORCED_BATCH_DEPTH.set(nextDepth);
                if (dirty) {
                    FORCED_BATCH_DIRTY.set(true);
                }
            }
        }
        return true;
    }

    /**
     * @return true if the forced change may proceed
     */
    public static boolean tryAllowForcedChange() {
        if (!VoteConfig.isEnvironmentCooldownEnabled()) {
            return true;
        }
        if (isNatural() || isSustainedForced() || isPassThrough()) {
            return true;
        }
        if (isForcedBatch()) {
            FORCED_BATCH_DIRTY.set(true);
            return true;
        }
        if (isOnCooldown()) {
            return false;
        }
        armCooldown();
        return true;
    }

    private static void armCooldown() {
        int seconds = VoteConfig.getEnvironmentCooldownSeconds();
        if (seconds <= 0) {
            cooldownUntilMs = 0L;
            return;
        }
        cooldownUntilMs = System.currentTimeMillis() + seconds * 1000L;
    }
}
