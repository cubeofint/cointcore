package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Stops Relics from walking Sophisticated Backpack contents every tick.
 * Relics worn directly in Curios / player inventory are unaffected.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code relics_backpack_scan}.
 */
public final class RelicsBackpackScanConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Mode {
        /** Never scan nested backpack inventories for relics. */
        SKIP_NESTED,
        /** Scan nested inventories only every N ticks. */
        THROTTLE
    }

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 200;

    private static boolean enabled = false;
    private static Mode mode = Mode.SKIP_NESTED;
    private static int scanIntervalTicks = 20;

    private RelicsBackpackScanConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Mode getMode() {
        return mode;
    }

    public static int getScanIntervalTicks() {
        return scanIntervalTicks;
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
                "Reloaded Relics backpack scan config (enabled={}, mode={}, interval={})",
                enabled,
                mode,
                scanIntervalTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        mode = loaded.mode();
        scanIntervalTicks = loaded.scanIntervalTicks();
    }

    private static LoadedConfig parse(Data data) {
        Mode parsed = Mode.SKIP_NESTED;
        if (data.mode != null && !data.mode.isBlank()) {
            try {
                parsed = Mode.valueOf(data.mode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                LOGGER.warn("Unknown relics_backpack_scan.mode '{}', using SKIP_NESTED", data.mode);
            }
        }
        return new LoadedConfig(
                data.enabled,
                parsed,
                clamp(data.scanIntervalTicks, MIN_INTERVAL, MAX_INTERVAL)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(boolean enabled, Mode mode, int scanIntervalTicks) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = false;

        /** {@code skip_nested} or {@code throttle}. */
        @SerializedName("mode")
        String mode = "skip_nested";

        @SerializedName("scan_interval_ticks")
        int scanIntervalTicks = 20;
    }
}
