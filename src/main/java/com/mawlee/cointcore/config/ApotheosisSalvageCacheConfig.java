package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Memoizes Apotheosis {@code SalvagingMenu.findMatch} per item + components. Pipes (Integrated Tunnels, etc.) offer
 * items to a Salvaging Table every tick and each offer scanned every salvaging recipe (4.6% of the server thread).
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code apotheosis_salvage_cache}. Defaults to enabled.
 */
public final class ApotheosisSalvageCacheConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;

    private ApotheosisSalvageCacheConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    static void applySection(Data data) {
        Data resolved = data != null ? data : defaultFileData();
        enabled = resolved.enabled;
    }

    static void logReload() {
        LOGGER.info("Reloaded Apotheosis salvage cache config (enabled={})", enabled);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;
    }
}
