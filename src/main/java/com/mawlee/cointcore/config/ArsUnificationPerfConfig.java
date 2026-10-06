package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Skips Ars Unification's full {@code processRecipes()} on per-player
 * datapack sync (login). Rebuild still runs once and on {@code /reload}.
 *
 * <p>File: {@code config/cointcore/ars-perf.json} section {@code unification}.
 */
public final class ArsUnificationPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean skipPerPlayerSync = true;

    private ArsUnificationPerfConfig() {
    }

    public static boolean isSkipPerPlayerSync() {
        return skipPerPlayerSync;
    }

    public static Path getConfigPath() {
        return ArsPerfConfigs.path();
    }

    public static void load() {
        ArsPerfConfigs.load();
    }

    public static boolean reload() {
        return ArsPerfConfigs.reload();
    }

    static void applySection(FileData data) {
        skipPerPlayerSync = data == null || data.skipPerPlayerSync == null || data.skipPerPlayerSync;
    }

    static void logReload() {
        LOGGER.info("Reloaded Ars Unification perf config (skipPerPlayerSync={})", skipPerPlayerSync);
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.skipPerPlayerSync = true;
        return data;
    }

    static final class FileData {
        @SerializedName("skipPerPlayerSync")
        Boolean skipPerPlayerSync;
    }
}
