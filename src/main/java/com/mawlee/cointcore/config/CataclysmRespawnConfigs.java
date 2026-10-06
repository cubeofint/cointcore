package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/cataclysm-respawn.json} with sections {@code sunken_city}
 * and {@code structures}.
 */
public final class CataclysmRespawnConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "cataclysm-respawn.json";

    private CataclysmRespawnConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            SunkenCityRespawnConfig.applySection(SunkenCityRespawnConfig.defaultFileData());
            CataclysmStructureRespawnConfig.applySection(CataclysmStructureRespawnConfig.defaultFileData());
        }
    }

    public static boolean reload() {
        try {
            applyRoot(readRoot(), true);
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to reload {}; keeping previous values", FILE_NAME, exception);
            return false;
        }
    }

    private static JsonObject readRoot() throws IOException {
        return ConfigMergeSupport.loadMergedObject(
                FILE_NAME,
                ConfigMergeSupport.legacyMap(
                        "sunken_city", "sunken-city-respawn.json",
                        "structures", "cataclysm-structure-respawn.json"
                ),
                CataclysmRespawnConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("sunken_city", ConfigMergeSupport.toJsonObject(SunkenCityRespawnConfig.defaultFileData()));
        root.add("structures", ConfigMergeSupport.toJsonObject(CataclysmStructureRespawnConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        SunkenCityRespawnConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "sunken_city", SunkenCityRespawnConfig.FileData.class, SunkenCityRespawnConfig::defaultFileData
        ));
        CataclysmStructureRespawnConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "structures", CataclysmStructureRespawnConfig.FileData.class,
                CataclysmStructureRespawnConfig::defaultFileData
        ));
        if (logReload) {
            SunkenCityRespawnConfig.logReload();
            CataclysmStructureRespawnConfig.logReload();
        }
    }
}
