package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Dedicated-server skip of Relics' {@code Item#appendHoverText} handler, which
 * calls {@code relics$processTooltip} stripped by {@code @OnlyIn(Dist.CLIENT)}.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code relics_server_tooltip}.
 */
public final class RelicsServerTooltipConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;

    private RelicsServerTooltipConfig() {
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
        LOGGER.info("Reloaded Relics dedicated-server tooltip config (enabled={})", enabled);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;
    }
}
