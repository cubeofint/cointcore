package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/cleanup.json} with sections {@code mobs} and {@code items}.
 */
public final class CleanupConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "cleanup.json";

    private CleanupConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            MobCleanupConfig.applySection(MobCleanupConfig.defaultFileData());
            WorldCleanupConfig.applySection(WorldCleanupConfig.defaultFileData());
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
                        "mobs", "mob-cleanup.json",
                        "items", "world-cleanup.json"
                ),
                CleanupConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("mobs", ConfigMergeSupport.toJsonObject(MobCleanupConfig.defaultFileData()));
        root.add("items", ConfigMergeSupport.toJsonObject(WorldCleanupConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        MobCleanupConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "mobs", MobCleanupConfig.FileData.class, MobCleanupConfig::defaultFileData
        ));
        WorldCleanupConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "items", WorldCleanupConfig.FileData.class, WorldCleanupConfig::defaultFileData
        ));
        if (logReload) {
            MobCleanupConfig.logReload();
            WorldCleanupConfig.logReload();
        }
    }
}
