package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Optional fast-path for NeoVitae {@code NVMissingMappings#rewriteSectionPalettes}.
 * NeoVitae's own {@code neovitae-server.toml} has no toggle for this rewrite.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section
 * {@code neovitae_missing_mappings_fastpath}.
 */
public final class NeovitaeMissingMappingsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;

    private NeovitaeMissingMappingsConfig() {
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
        LOGGER.info("Reloaded NeoVitae missing-mappings fastpath (enabled={})", enabled);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;
    }
}
