package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles Super Factory Manager timers and optionally soft-skips program ticks under high MSPT.
 * <p>
 * Hard-skipping every program tick when MSPT ≥ threshold permanently broke SFM on laggy servers
 * (avg MSPT often &gt; 40). MSPT gate is off by default; when enabled it only runs 1 of N ticks.
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code sfm}.
 */
public final class SfmPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_TIMER_INTERVAL = 1;
    private static final int MAX_TIMER_INTERVAL = 200;
    private static final double MIN_MSPT_SKIP = 0.0D;
    private static final double MAX_MSPT_SKIP = 1000.0D;
    private static final int MIN_MSPT_ALLOW_EVERY = 1;
    private static final int MAX_MSPT_ALLOW_EVERY = 64;

    private static boolean enabled = true;
    private static int minTimerIntervalTicks = 5;
    /** 0 = disabled (default). Legacy hard-skip at 40 broke SFM under load. */
    private static double msptSkipThreshold = 0.0D;
    /** When MSPT ≥ threshold, allow Program.tick only once every N manager ticks. */
    private static int msptSkipAllowEvery = 4;

    private SfmPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Timers with a shorter interval (e.g. {@code every tick}) are raised to at least this many ticks.
     */
    public static int getMinTimerIntervalTicks() {
        return minTimerIntervalTicks;
    }

    /**
     * When &gt; 0 and rolling MSPT mean (1m) is at/above this value, soft-throttle {@code Program.tick}.
     */
    public static double getMsptSkipThreshold() {
        return msptSkipThreshold;
    }

    public static int getMsptSkipAllowEvery() {
        return msptSkipAllowEvery;
    }

    public static int clampTimerInterval(int ticks) {
        if (!enabled) {
            return ticks;
        }
        int min = minTimerIntervalTicks;
        if (min <= 1 || ticks >= min) {
            return ticks;
        }
        return min;
    }

    /**
     * @param managerTick {@link ca.teamdman.sfm.common.blockentity.ManagerBlockEntity#getTick()}
     */
    public static boolean shouldSkipProgramTick(int managerTick) {
        if (!enabled) {
            return false;
        }
        double threshold = msptSkipThreshold;
        if (threshold <= 0.0D) {
            return false;
        }
        if (!PerfTickCache.isMsptAtLeast(threshold)) {
            return false;
        }
        int every = Math.max(1, msptSkipAllowEvery);
        // Soft throttle: still run 1 of every N manager ticks so factories keep working under load.
        return Math.floorMod(managerTick, every) != 0;
    }

    public static Path getConfigPath() {
        return TickThrottleConfigs.path();
    }

    public static void load() {
        TickThrottleConfigs.load();
    }

    public static boolean reload() {
        return TickThrottleConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded SFM perf config (enabled={} minTimerInterval={} msptSkipThreshold={} msptSkipAllowEvery={})",
                enabled,
                minTimerIntervalTicks,
                msptSkipThreshold,
                msptSkipAllowEvery
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        minTimerIntervalTicks = loaded.minTimerIntervalTicks();
        msptSkipThreshold = loaded.msptSkipThreshold();
        msptSkipAllowEvery = loaded.msptSkipAllowEvery();
        if (msptSkipThreshold > 0.0D) {
            LOGGER.info(
                    "SFM MSPT soft-throttle active (threshold={}ms allowEvery={}): Program.tick runs 1/{} ticks when MSPT is high",
                    msptSkipThreshold,
                    msptSkipAllowEvery,
                    msptSkipAllowEvery
            );
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        int minInterval = clamp(
                data.minTimerIntervalTicks != null ? data.minTimerIntervalTicks : 5,
                MIN_TIMER_INTERVAL,
                MAX_TIMER_INTERVAL
        );
        // Default off. Legacy files often had 40 which hard-disabled SFM on laggy servers.
        double msptSkip = clampDouble(
                data.msptSkipThreshold != null ? data.msptSkipThreshold : 0.0D,
                MIN_MSPT_SKIP,
                MAX_MSPT_SKIP
        );
        int allowEvery = clamp(
                data.msptSkipAllowEvery != null ? data.msptSkipAllowEvery : 4,
                MIN_MSPT_ALLOW_EVERY,
                MAX_MSPT_ALLOW_EVERY
        );
        // Migrate old hard-skip configs: threshold 40 permanently broke SFM when MSPT was high.
        if (data.msptSkipThreshold != null
                && data.msptSkipThreshold == 40.0D
                && data.msptSkipAllowEvery == null) {
            msptSkip = 0.0D;
            LOGGER.warn(
                    "SFM perf: legacy msptSkipThreshold=40 disabled (it cancelled Program.tick whenever MSPT≥40). "
                            + "To re-enable soft throttle, set msptSkipThreshold (e.g. 80) and msptSkipAllowEvery (e.g. 4)"
            );
        }
        return new LoadedConfig(on, minInterval, msptSkip, allowEvery);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.minTimerIntervalTicks = 5;
        data.msptSkipThreshold = 0.0D;
        data.msptSkipAllowEvery = 4;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int minTimerIntervalTicks,
            double msptSkipThreshold,
            int msptSkipAllowEvery
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("minTimerIntervalTicks")
        private Integer minTimerIntervalTicks;

        @SerializedName("msptSkipThreshold")
        private Double msptSkipThreshold;

        @SerializedName("msptSkipAllowEvery")
        private Integer msptSkipAllowEvery;
    }
}
