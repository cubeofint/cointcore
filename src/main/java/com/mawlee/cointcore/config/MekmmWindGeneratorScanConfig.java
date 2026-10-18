package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Replaces Mekanism MoreMachine large wind generator's full-height
 * {@code getBlockState} neighbour scan with a loaded-chunk block-entity walk.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code mekmm_wind_generator_scan}.
 * Defaults to enabled: the replacement matches original radius/result/generation
 * and removes a multi-millisecond server stall per generator.
 */
public final class MekmmWindGeneratorScanConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_CACHE_INTERVAL = 0;
    private static final int MAX_CACHE_INTERVAL = 200;

    private static boolean enabled = true;
    private static int cacheIntervalTicks = 200;

    private MekmmWindGeneratorScanConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getCacheIntervalTicks() {
        return cacheIntervalTicks;
    }

    public static Path getConfigPath() {
        return LagFixesConfigs.path();
    }

    public static void load() {
        LagFixesConfigs.load();
    }

    public static boolean reload() {
        return LagFixesConfigs.reload();
    }

    static void applySection(Data data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded mekmm wind generator scan config (enabled={}, cache_interval_ticks={})",
                enabled,
                cacheIntervalTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        cacheIntervalTicks = loaded.cacheIntervalTicks();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                clamp(data.cacheIntervalTicks, MIN_CACHE_INTERVAL, MAX_CACHE_INTERVAL)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(boolean enabled, int cacheIntervalTicks) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        /**
         * Extra in-mixin cache on top of mekmm's own 20–200 tick cooldown.
         * {@code 0} disables the extra cache.
         */
        @SerializedName("cache_interval_ticks")
        int cacheIntervalTicks = 200;
    }
}
