package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/storage-perf.json} with sections {@code ae2} and {@code rs_importer}.
 */
public final class StoragePerfConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "storage-perf.json";

    private StoragePerfConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            Ae2PerfConfig.applySection(Ae2PerfConfig.defaultFileData());
            RsImporterPerfConfig.applySection(RsImporterPerfConfig.defaultFileData());
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
                        "ae2", "ae2_perf.json",
                        "rs_importer", "rs_importer_perf.json"
                ),
                StoragePerfConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("ae2", ConfigMergeSupport.toJsonObject(Ae2PerfConfig.defaultFileData()));
        root.add("rs_importer", ConfigMergeSupport.toJsonObject(RsImporterPerfConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        Ae2PerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "ae2", Ae2PerfConfig.Data.class, Ae2PerfConfig::defaultFileData
        ));
        RsImporterPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "rs_importer", RsImporterPerfConfig.Data.class, RsImporterPerfConfig::defaultFileData
        ));
        if (logReload) {
            Ae2PerfConfig.logReload();
            RsImporterPerfConfig.logReload();
        }
    }
}
