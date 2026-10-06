package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles Botany Pots hopper export into the inventory below the pot.
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code botanypots}.
 */
public final class BotanyPotsPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 1200;

    private static boolean enabled = true;
    private static int minExportIntervalTicks = 8;
    private static int fullInventoryBackoffTicks = 100;

    private BotanyPotsPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Minimum delay between export attempts when inserts make progress (or storage is empty).
     */
    public static int getMinExportIntervalTicks() {
        return minExportIntervalTicks;
    }

    /**
     * Delay used when every insert attempt fails (destination full / rejects items).
     */
    public static int getFullInventoryBackoffTicks() {
        return fullInventoryBackoffTicks;
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
                "Reloaded Botany Pots perf config (enabled={} minExportInterval={} fullBackoff={})",
                enabled,
                minExportIntervalTicks,
                fullInventoryBackoffTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        minExportIntervalTicks = loaded.minExportIntervalTicks();
        fullInventoryBackoffTicks = loaded.fullInventoryBackoffTicks();
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        int minInterval = clamp(
                data.minExportIntervalTicks != null ? data.minExportIntervalTicks : 8,
                MIN_INTERVAL,
                MAX_INTERVAL
        );
        int backoff = clamp(
                data.fullInventoryBackoffTicks != null ? data.fullInventoryBackoffTicks : 100,
                MIN_INTERVAL,
                MAX_INTERVAL
        );
        if (backoff < minInterval) {
            backoff = minInterval;
        }
        return new LoadedConfig(on, minInterval, backoff);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.minExportIntervalTicks = 8;
        data.fullInventoryBackoffTicks = 100;
        return data;
    }

    private record LoadedConfig(boolean enabled, int minExportIntervalTicks, int fullInventoryBackoffTicks) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("minExportIntervalTicks")
        private Integer minExportIntervalTicks;

        @SerializedName("fullInventoryBackoffTicks")
        private Integer fullInventoryBackoffTicks;
    }
}
