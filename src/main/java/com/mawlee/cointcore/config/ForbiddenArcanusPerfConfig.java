package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Throttles Forbidden Arcanus {@code hasEffect} equipment scans (EffectGrantingRule).
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code forbidden_arcanus}.
 */
public final class ForbiddenArcanusPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double MIN_MSPT_SKIP = 0.0D;
    private static final double MAX_MSPT_SKIP = 1000.0D;

    private static boolean enabled = true;
    private static boolean skipNonPlayers = true;
    private static boolean tickCachePlayers = true;
    private static double msptSkipThreshold = 80.0D;
    private static boolean skipPlayersWhenMsptHigh = true;

    private ForbiddenArcanusPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isSkipNonPlayers() {
        return skipNonPlayers;
    }

    public static boolean isTickCachePlayers() {
        return tickCachePlayers;
    }

    /**
     * When &gt; 0 and rolling MSPT mean (1m) is at/above this value, skip FA hasEffect scans
     * for non-players (and players if {@link #isSkipPlayersWhenMsptHigh()}).
     */
    public static double getMsptSkipThreshold() {
        return msptSkipThreshold;
    }

    public static boolean isSkipPlayersWhenMsptHigh() {
        return skipPlayersWhenMsptHigh;
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
                "Reloaded Forbidden Arcanus perf config (enabled={}, skipNonPlayers={}, tickCachePlayers={}, msptSkipThreshold={}, skipPlayersWhenMsptHigh={})",
                enabled,
                skipNonPlayers,
                tickCachePlayers,
                msptSkipThreshold,
                skipPlayersWhenMsptHigh
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        skipNonPlayers = loaded.skipNonPlayers();
        tickCachePlayers = loaded.tickCachePlayers();
        msptSkipThreshold = loaded.msptSkipThreshold();
        skipPlayersWhenMsptHigh = loaded.skipPlayersWhenMsptHigh();
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        boolean skipMobs = data.skipNonPlayers == null || data.skipNonPlayers;
        boolean cache = data.tickCachePlayers == null || data.tickCachePlayers;
        double threshold = clamp(
                data.msptSkipThreshold != null ? data.msptSkipThreshold : 80.0D,
                MIN_MSPT_SKIP,
                MAX_MSPT_SKIP
        );
        // Absent field → true (prod default). Explicit false in JSON still disables.
        boolean skipPlayers = data.skipPlayersWhenMsptHigh == null || data.skipPlayersWhenMsptHigh;
        return new LoadedConfig(on, skipMobs, cache, threshold, skipPlayers);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.skipNonPlayers = true;
        data.tickCachePlayers = true;
        data.msptSkipThreshold = 80.0D;
        data.skipPlayersWhenMsptHigh = true;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            boolean skipNonPlayers,
            boolean tickCachePlayers,
            double msptSkipThreshold,
            boolean skipPlayersWhenMsptHigh
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("skipNonPlayers")
        private Boolean skipNonPlayers;

        @SerializedName("tickCachePlayers")
        private Boolean tickCachePlayers;

        @SerializedName("msptSkipThreshold")
        private Double msptSkipThreshold;

        @SerializedName("skipPlayersWhenMsptHigh")
        private Boolean skipPlayersWhenMsptHigh;
    }
}
