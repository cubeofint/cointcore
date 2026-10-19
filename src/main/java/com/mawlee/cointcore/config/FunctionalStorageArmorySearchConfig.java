package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Armory Cabinet search: match registry id / path / description id / custom name
 * instead of {@code ItemStack#getTooltipLines} (which crashes with Relics).
 * Applied on client and server so filtered slots stay in sync.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code functionalstorage_armory_search}.
 */
public final class FunctionalStorageArmorySearchConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;

    private FunctionalStorageArmorySearchConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
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
        Data resolved = data != null ? data : defaultFileData();
        enabled = resolved.enabled;
    }

    static void logReload() {
        LOGGER.info("Reloaded Functional Storage armory search config (enabled={})", enabled);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;
    }
}
