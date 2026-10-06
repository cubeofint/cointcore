package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles Powah Player Transmitter (wireless charger) inventory scans.
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code powah}.
 */
public final class PowahPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_TICKS = 1;
    private static final int MAX_TICKS = 200;
    private static final double MIN_MSPT = 0.0D;
    private static final double MAX_MSPT = 1000.0D;

    private static boolean enabled = true;
    private static int intervalTicks = 5;
    private static int dimIntervalTicks = 10;
    private static int idleSkipTicks = 20;
    private static int highMsptIntervalTicks = 10;
    private static int highMsptDimIntervalTicks = 20;
    private static int highMsptIdleSkipTicks = 40;
    private static double highMsptThreshold = 60.0D;

    private PowahPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int resolveIntervalTicks(boolean multiDim) {
        boolean highMspt = PerfTickCache.isMsptAtLeast(highMsptThreshold);
        if (multiDim) {
            return highMspt ? highMsptDimIntervalTicks : dimIntervalTicks;
        }
        return highMspt ? highMsptIntervalTicks : intervalTicks;
    }

    public static int resolveIdleSkipTicks() {
        if (PerfTickCache.isMsptAtLeast(highMsptThreshold)) {
            return highMsptIdleSkipTicks;
        }
        return idleSkipTicks;
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

    static void applySection(Data data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded Powah perf (enabled={}, interval={}, dimInterval={}, idleSkip={}, highMsptInterval={}, highMsptDimInterval={}, highMsptIdleSkip={}, msptThreshold={})",
                enabled,
                intervalTicks,
                dimIntervalTicks,
                idleSkipTicks,
                highMsptIntervalTicks,
                highMsptDimIntervalTicks,
                highMsptIdleSkipTicks,
                highMsptThreshold
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        intervalTicks = loaded.intervalTicks();
        dimIntervalTicks = loaded.dimIntervalTicks();
        idleSkipTicks = loaded.idleSkipTicks();
        highMsptIntervalTicks = loaded.highMsptIntervalTicks();
        highMsptDimIntervalTicks = loaded.highMsptDimIntervalTicks();
        highMsptIdleSkipTicks = loaded.highMsptIdleSkipTicks();
        highMsptThreshold = loaded.highMsptThreshold();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                clamp(data.intervalTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.dimIntervalTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.idleSkipTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.highMsptIntervalTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.highMsptDimIntervalTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.highMsptIdleSkipTicks, MIN_TICKS, MAX_TICKS),
                clamp(data.highMsptThreshold, MIN_MSPT, MAX_MSPT)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(
            boolean enabled,
            int intervalTicks,
            int dimIntervalTicks,
            int idleSkipTicks,
            int highMsptIntervalTicks,
            int highMsptDimIntervalTicks,
            int highMsptIdleSkipTicks,
            double highMsptThreshold
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Minimum ticks between charge attempts (same-dimension card). */
        @SerializedName("interval_ticks")
        int intervalTicks = 5;

        /** Interval for interdimensional binding cards. */
        @SerializedName("dim_interval_ticks")
        int dimIntervalTicks = 10;

        /** Extra skip after a charge attempt that moved 0 FE. */
        @SerializedName("idle_skip_ticks")
        int idleSkipTicks = 20;

        @SerializedName("high_mspt_interval_ticks")
        int highMsptIntervalTicks = 10;

        @SerializedName("high_mspt_dim_interval_ticks")
        int highMsptDimIntervalTicks = 20;

        @SerializedName("high_mspt_idle_skip_ticks")
        int highMsptIdleSkipTicks = 40;

        @SerializedName("high_mspt_threshold")
        double highMsptThreshold = 60.0D;
    }
}
