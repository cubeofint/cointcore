package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles Refined Storage importer work when transfers idle (nothing moved),
 * avoiding per-tick SIMULATE extract/insert against the network.
 *
 * <p>File: {@code config/cointcore/storage-perf.json} section {@code rs_importer}.
 */
public final class RsImporterPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_IDLE_TICKS = 1;
    private static final int MAX_IDLE_TICKS = 200;
    private static final double MIN_MSPT = 0.0D;
    private static final double MAX_MSPT = 1000.0D;

    private static boolean enabled = true;
    private static int idleSkipTicks = 20;
    private static int highMsptIdleSkipTicks = 40;
    private static double highMsptThreshold = 60.0D;

    private RsImporterPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getIdleSkipTicks() {
        return idleSkipTicks;
    }

    public static int getHighMsptIdleSkipTicks() {
        return highMsptIdleSkipTicks;
    }

    public static double getHighMsptThreshold() {
        return highMsptThreshold;
    }

    /**
     * Idle skip length for this server tick (MSPT resolved once via {@link com.mawlee.cointcore.spark.PerfTickCache}).
     */
    public static int resolveIdleSkipTicksForCurrentTick() {
        if (PerfTickCache.isMsptAtLeast(highMsptThreshold)) {
            return highMsptIdleSkipTicks;
        }
        return idleSkipTicks;
    }

    public static Path getConfigPath() {
        return StoragePerfConfigs.path();
    }

    public static void load() {
        StoragePerfConfigs.load();
    }

    public static boolean reload() {
        return StoragePerfConfigs.reload();
    }

    static void applySection(Data data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded RS importer perf (enabled={}, idleSkip={}, highMsptIdleSkip={}, msptThreshold={})",
                enabled,
                idleSkipTicks,
                highMsptIdleSkipTicks,
                highMsptThreshold
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        idleSkipTicks = loaded.idleSkipTicks();
        highMsptIdleSkipTicks = loaded.highMsptIdleSkipTicks();
        highMsptThreshold = loaded.highMsptThreshold();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                clamp(data.idleSkipTicks, MIN_IDLE_TICKS, MAX_IDLE_TICKS),
                clamp(data.highMsptIdleSkipTicks, MIN_IDLE_TICKS, MAX_IDLE_TICKS),
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
            int idleSkipTicks,
            int highMsptIdleSkipTicks,
            double highMsptThreshold
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Ticks to skip doWork after a transfer that moved nothing. */
        @SerializedName("idle_skip_ticks")
        int idleSkipTicks = 20;

        /** Stronger skip when rolling MSPT is at/above threshold. */
        @SerializedName("high_mspt_idle_skip_ticks")
        int highMsptIdleSkipTicks = 40;

        @SerializedName("high_mspt_threshold")
        double highMsptThreshold = 60.0D;
    }
}
