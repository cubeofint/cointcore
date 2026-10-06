package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.enderio.ItemConduitNetworkThrottle;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Soft-throttles EnderIO item conduit networks that scan huge inventories every tick.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code enderio_item_conduits}.
 */
public final class EnderIoItemConduitConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 200;
    private static final int MIN_SLOTS = 1;
    private static final int MAX_SLOTS = 4096;
    private static final int MIN_BACKOFF = 1;
    private static final int MAX_BACKOFF = 400;

    private static boolean enabled = false;
    private static int tickInterval = 10;
    private static int maxSlotsPerPass = 64;
    private static int idleBackoffTicks = 40;
    private static boolean skipUnchangedInventories = true;

    private EnderIoItemConduitConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getTickInterval() {
        return tickInterval;
    }

    public static int getMaxSlotsPerPass() {
        return maxSlotsPerPass;
    }

    public static int getIdleBackoffTicks() {
        return idleBackoffTicks;
    }

    public static boolean isSkipUnchangedInventories() {
        return skipUnchangedInventories;
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
        ItemConduitNetworkThrottle.clearAll();
        LOGGER.info(
                "Reloaded EnderIO item conduit config (enabled={}, interval={}, maxSlots={}, idleBackoff={}, skipUnchanged={})",
                enabled,
                tickInterval,
                maxSlotsPerPass,
                idleBackoffTicks,
                skipUnchangedInventories
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        tickInterval = loaded.tickInterval();
        maxSlotsPerPass = loaded.maxSlotsPerPass();
        idleBackoffTicks = loaded.idleBackoffTicks();
        skipUnchangedInventories = loaded.skipUnchangedInventories();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                clamp(data.tickInterval, MIN_INTERVAL, MAX_INTERVAL),
                clamp(data.maxSlotsPerPass, MIN_SLOTS, MAX_SLOTS),
                clamp(data.idleBackoffTicks, MIN_BACKOFF, MAX_BACKOFF),
                data.skipUnchangedInventories
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(
            boolean enabled,
            int tickInterval,
            int maxSlotsPerPass,
            int idleBackoffTicks,
            boolean skipUnchangedInventories
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = false;

        /** Minimum ticks between full extraction passes when work is happening. */
        @SerializedName("tick_interval")
        int tickInterval = 10;

        /** Cap on extract-handler slots scanned per pass (fair round-robin offset). */
        @SerializedName("max_slots_per_pass")
        int maxSlotsPerPass = 64;

        /** Extra delay after a pass that moved nothing. */
        @SerializedName("idle_backoff_ticks")
        int idleBackoffTicks = 40;

        /** Skip a pass when the extract inventory fingerprint is unchanged and last pass was idle. */
        @SerializedName("skip_unchanged_inventories")
        boolean skipUnchangedInventories = true;
    }
}
