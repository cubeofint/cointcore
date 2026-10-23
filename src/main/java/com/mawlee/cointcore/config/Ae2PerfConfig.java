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
    private static int storageCacheIntervalTicks = 5;
    private static boolean sophisticatedFastExtract = true;

    private Ae2PerfConfig() {
    }

    /** Storage buses extract from Sophisticated Storage through its slot index instead of walking every slot. */
    public static boolean isSophisticatedFastExtract() {
        return sophisticatedFastExtract;
    }

    public static boolean isCacheFuzzySearchMaxValue() {
        return cacheFuzzySearchMaxValue;
    }

    /**
     * How often a grid with stack watchers (level emitters, open terminals, …) rebuilds its cached inventory;
     * AE2 does it every tick. 1 = vanilla AE2 behaviour.
     */
    public static int getStorageCacheIntervalTicks() {
        return storageCacheIntervalTicks;
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
                "Reloaded AE2 perf config (cacheFuzzySearchMaxValue={}, storageCacheIntervalTicks={})",
                cacheFuzzySearchMaxValue,
                storageCacheIntervalTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        cacheFuzzySearchMaxValue = loaded.cacheFuzzySearchMaxValue();
        storageCacheIntervalTicks = loaded.storageCacheIntervalTicks();
        sophisticatedFastExtract = loaded.sophisticatedFastExtract();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(data.cacheFuzzySearchMaxValue, Math.max(1, Math.min(40, data.storageCacheIntervalTicks)),
                data.sophisticatedFastExtract);
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(boolean cacheFuzzySearchMaxValue, int storageCacheIntervalTicks,
                                boolean sophisticatedFastExtract) {
    }

    static final class Data {
        @SerializedName("cache_fuzzy_search_max_value")
        boolean cacheFuzzySearchMaxValue = true;

        /** Ticks between cached-inventory rebuilds of grids with watchers (AE2: every tick). 1 disables the throttle. */
        @SerializedName("storage_cache_interval_ticks")
        int storageCacheIntervalTicks = 5;

        /** Storage-bus extraction from Sophisticated Storage via its slot index. */
        @SerializedName("sophisticated_fast_extract")
        boolean sophisticatedFastExtract = true;
    }
}
