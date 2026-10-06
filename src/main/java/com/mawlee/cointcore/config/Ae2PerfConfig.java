package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * AE2 hot-path tweaks. Primary: cache {@code AEItemKey.getFuzzySearchMaxValue()} so
 * KeyCounter/storage scans stop hammering ItemStack DataComponents every call.
 *
 * <p>File: {@code config/cointcore/storage-perf.json} section {@code ae2}.
 */
public final class Ae2PerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean cacheFuzzySearchMaxValue = true;

    private Ae2PerfConfig() {
    }

    public static boolean isCacheFuzzySearchMaxValue() {
        return cacheFuzzySearchMaxValue;
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
                "Reloaded AE2 perf config (cacheFuzzySearchMaxValue={})",
                cacheFuzzySearchMaxValue
        );
    }

    private static void apply(LoadedConfig loaded) {
        cacheFuzzySearchMaxValue = loaded.cacheFuzzySearchMaxValue();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(data.cacheFuzzySearchMaxValue);
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(boolean cacheFuzzySearchMaxValue) {
    }

    static final class Data {
        @SerializedName("cache_fuzzy_search_max_value")
        boolean cacheFuzzySearchMaxValue = true;
    }
}
