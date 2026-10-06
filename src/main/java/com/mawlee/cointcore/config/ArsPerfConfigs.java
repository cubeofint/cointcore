package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/ars-perf.json} with sections {@code glyph} and {@code unification}.
 */
public final class ArsPerfConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "ars-perf.json";

    private ArsPerfConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            ArsGlyphPerfConfig.applySection(ArsGlyphPerfConfig.defaultFileData());
            ArsUnificationPerfConfig.applySection(ArsUnificationPerfConfig.defaultFileData());
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
                        "glyph", "ars_glyph_perf.json",
                        "unification", "ars_unification_perf.json"
                ),
                ArsPerfConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("glyph", ConfigMergeSupport.toJsonObject(ArsGlyphPerfConfig.defaultFileData()));
        root.add("unification", ConfigMergeSupport.toJsonObject(ArsUnificationPerfConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        ArsGlyphPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "glyph", ArsGlyphPerfConfig.FileData.class, ArsGlyphPerfConfig::defaultFileData
        ));
        ArsUnificationPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "unification", ArsUnificationPerfConfig.FileData.class, ArsUnificationPerfConfig::defaultFileData
        ));
        if (logReload) {
            ArsGlyphPerfConfig.logReload();
            ArsUnificationPerfConfig.logReload();
        }
    }
}
