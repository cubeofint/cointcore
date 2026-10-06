package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles AdvancedAE Quantum Armor passive upgrades that hit ME storage / world scans
 * every inventory tick (autoStock, magnet, autoFeed, recharging).
 *
 * <p>File: {@code config/cointcore/storage-perf.json} section {@code advanced_ae}.
 */
public final class AdvancedAePerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 200;

    private static boolean enabled = true;
    /** Full {@code MEStorage.getAvailableStacks()} — default 40 ticks (~2/s). */
    private static int autoStockIntervalTicks = 40;
    /** Item/XP entity AABB scan — default 5 ticks. */
    private static int magnetIntervalTicks = 5;
    /** ME extract when hungry — default 20 ticks. */
    private static int autoFeedIntervalTicks = 20;
    /** Recharge inventory/curios from ME — default 10 ticks. */
    private static int rechargingIntervalTicks = 10;

    private AdvancedAePerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getAutoStockIntervalTicks() {
        return autoStockIntervalTicks;
    }

    public static int getMagnetIntervalTicks() {
        return magnetIntervalTicks;
    }

    public static int getAutoFeedIntervalTicks() {
        return autoFeedIntervalTicks;
    }

    public static int getRechargingIntervalTicks() {
        return rechargingIntervalTicks;
    }

    /**
     * @return {@code true} if the upgrade should run on this game tick.
     */
    public static boolean shouldRunThisTick(Level level, int intervalTicks) {
        if (!enabled || level == null || intervalTicks <= 1) {
            return true;
        }
        return Math.floorMod(level.getGameTime(), intervalTicks) == 0;
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

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded AdvancedAE perf config (enabled={}, autoStock={}, magnet={}, autoFeed={}, recharging={})",
                enabled,
                autoStockIntervalTicks,
                magnetIntervalTicks,
                autoFeedIntervalTicks,
                rechargingIntervalTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        autoStockIntervalTicks = loaded.autoStockIntervalTicks();
        magnetIntervalTicks = loaded.magnetIntervalTicks();
        autoFeedIntervalTicks = loaded.autoFeedIntervalTicks();
        rechargingIntervalTicks = loaded.rechargingIntervalTicks();
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        return new LoadedConfig(
                on,
                clampInterval(data.autoStockIntervalTicks, 40),
                clampInterval(data.magnetIntervalTicks, 5),
                clampInterval(data.autoFeedIntervalTicks, 20),
                clampInterval(data.rechargingIntervalTicks, 10)
        );
    }

    private static int clampInterval(Integer value, int defaultValue) {
        int raw = value != null ? value : defaultValue;
        return Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, raw));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.autoStockIntervalTicks = 40;
        data.magnetIntervalTicks = 5;
        data.autoFeedIntervalTicks = 20;
        data.rechargingIntervalTicks = 10;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int autoStockIntervalTicks,
            int magnetIntervalTicks,
            int autoFeedIntervalTicks,
            int rechargingIntervalTicks
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("autoStockIntervalTicks")
        private Integer autoStockIntervalTicks;

        @SerializedName("magnetIntervalTicks")
        private Integer magnetIntervalTicks;

        @SerializedName("autoFeedIntervalTicks")
        private Integer autoFeedIntervalTicks;

        @SerializedName("rechargingIntervalTicks")
        private Integer rechargingIntervalTicks;
    }
}
