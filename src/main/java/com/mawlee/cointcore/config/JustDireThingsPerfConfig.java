package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.justdirethings.JustDireThingsAreaScanCache;
import com.mawlee.cointcore.justdirethings.JustDireThingsBlockValidCache;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Soft-throttles Just Dire Things area machines: keeps BaseMachineBE chunk protection
 * cache across ticks, reuses expensive {@code betweenClosed} area scans, and caches
 * BlockBreaker T2 {@code isBlockValid}/{@code getDrops} checks for unchanged blocks.
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code justdirethings}.
 */
public final class JustDireThingsPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 200;
    private static final double MIN_MSPT = 0.0D;
    private static final double MAX_MSPT = 1000.0D;

    private static boolean enabled = true;
    /** How often {@code clearProtectionCache} is allowed to run (vanilla = every tick). */
    private static int protectionCacheClearIntervalTicks = 20;
    /** Reuse area-scan results for this many game ticks. */
    private static int areaScanCacheTicks = 10;
    private static int highMsptAreaScanCacheTicks = 20;
    /** Reuse BlockBreaker T2 isBlockValid/getDrops results while BlockState+filter+tool match. */
    private static int blockValidCacheTicks = 40;
    private static int highMsptBlockValidCacheTicks = 100;
    private static double highMsptThreshold = 60.0D;

    private JustDireThingsPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getProtectionCacheClearIntervalTicks() {
        return protectionCacheClearIntervalTicks;
    }

    public static int resolveAreaScanCacheTicks() {
        if (PerfTickCache.isMsptAtLeast(highMsptThreshold)) {
            return highMsptAreaScanCacheTicks;
        }
        return areaScanCacheTicks;
    }

    public static int resolveBlockValidCacheTicks() {
        if (PerfTickCache.isMsptAtLeast(highMsptThreshold)) {
            return highMsptBlockValidCacheTicks;
        }
        return blockValidCacheTicks;
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
        JustDireThingsAreaScanCache.clearAll();
        JustDireThingsBlockValidCache.clearAll();
        LOGGER.info(
                "Reloaded Just Dire Things perf (enabled={}, protectionClearEvery={}, areaScanCache={}, highMsptCache={}, blockValidCache={}, highMsptBlockValid={}, msptThreshold={})",
                enabled,
                protectionCacheClearIntervalTicks,
                areaScanCacheTicks,
                highMsptAreaScanCacheTicks,
                blockValidCacheTicks,
                highMsptBlockValidCacheTicks,
                highMsptThreshold
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        protectionCacheClearIntervalTicks = loaded.protectionCacheClearIntervalTicks();
        areaScanCacheTicks = loaded.areaScanCacheTicks();
        highMsptAreaScanCacheTicks = loaded.highMsptAreaScanCacheTicks();
        blockValidCacheTicks = loaded.blockValidCacheTicks();
        highMsptBlockValidCacheTicks = loaded.highMsptBlockValidCacheTicks();
        highMsptThreshold = loaded.highMsptThreshold();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                clamp(data.protectionCacheClearIntervalTicks, MIN_INTERVAL, MAX_INTERVAL),
                clamp(data.areaScanCacheTicks, 0, MAX_INTERVAL),
                clamp(data.highMsptAreaScanCacheTicks, 0, MAX_INTERVAL),
                clamp(data.blockValidCacheTicks, 0, MAX_INTERVAL),
                clamp(data.highMsptBlockValidCacheTicks, 0, MAX_INTERVAL),
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
            int protectionCacheClearIntervalTicks,
            int areaScanCacheTicks,
            int highMsptAreaScanCacheTicks,
            int blockValidCacheTicks,
            int highMsptBlockValidCacheTicks,
            double highMsptThreshold
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        @SerializedName("protectionCacheClearIntervalTicks")
        int protectionCacheClearIntervalTicks = 20;

        @SerializedName("areaScanCacheTicks")
        int areaScanCacheTicks = 10;

        @SerializedName("highMsptAreaScanCacheTicks")
        int highMsptAreaScanCacheTicks = 20;

        @SerializedName("blockValidCacheTicks")
        int blockValidCacheTicks = 40;

        @SerializedName("highMsptBlockValidCacheTicks")
        int highMsptBlockValidCacheTicks = 100;

        @SerializedName("highMsptThreshold")
        double highMsptThreshold = 60.0D;
    }
}
