package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mawlee.cointcore.CointCore;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared helpers for multi-section JSON configs with one-shot legacy-file migration.
 */
final class ConfigMergeSupport {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ConfigMergeSupport() {
    }

    static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID);
    }

    static Path path(String fileName) {
        return dir().resolve(fileName);
    }

    /**
     * Loads a merged JSON object. If missing, builds from legacy section files (when present)
     * or {@code defaults}, writes the merged file, and renames legacy files to {@code .migrated}.
     */
    static JsonObject loadMergedObject(
            String mergedFileName,
            Map<String, String> sectionToLegacyFile,
            Supplier<JsonObject> defaults,
            boolean reloading,
            Logger logger
    ) throws IOException {
        Path merged = path(mergedFileName);
        Files.createDirectories(merged.getParent());

        if (Files.exists(merged)) {
            return readObject(merged);
        }

        JsonObject root = defaults.get();
        boolean migratedAny = false;
        for (Map.Entry<String, String> entry : sectionToLegacyFile.entrySet()) {
            Path legacy = path(entry.getValue());
            if (!Files.exists(legacy)) {
                continue;
            }
            try {
                JsonObject section = readObject(legacy);
                root.add(entry.getKey(), section);
                migratedAny = true;
                archiveLegacy(legacy, logger);
            } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
                logger.warn("Failed to migrate legacy config {} into {}", legacy.getFileName(), mergedFileName, exception);
            }
        }

        writeObject(merged, root);
        if (migratedAny) {
            logger.info("Migrated legacy configs into {}", merged);
        } else {
            logger.info("Created default config at {}", merged);
        }
        return root;
    }

    static <T> T sectionOrDefault(JsonObject root, String key, Class<T> type, Supplier<T> defaults) {
        JsonElement element = root.get(key);
        if (element == null || element.isJsonNull()) {
            return defaults.get();
        }
        T parsed = GSON.fromJson(element, type);
        return parsed != null ? parsed : defaults.get();
    }

    static JsonObject toJsonObject(Object data) {
        return GSON.toJsonTree(data).getAsJsonObject();
    }

    static Map<String, String> legacyMap(String... sectionAndFilePairs) {
        if (sectionAndFilePairs.length % 2 != 0) {
            throw new IllegalArgumentException("section/file pairs required");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < sectionAndFilePairs.length; i += 2) {
            map.put(sectionAndFilePairs[i], sectionAndFilePairs[i + 1]);
        }
        return map;
    }

    private static JsonObject readObject(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                throw new JsonSyntaxException("Expected JSON object in " + path);
            }
            return element.getAsJsonObject();
        }
    }

    private static void writeObject(Path path, JsonObject object) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(object, writer);
        }
    }

    private static void archiveLegacy(Path legacy, Logger logger) {
        try {
            Path archived = legacy.resolveSibling(legacy.getFileName().toString() + ".migrated");
            Files.move(legacy, archived, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Archived legacy config {} -> {}", legacy.getFileName(), archived.getFileName());
        } catch (IOException exception) {
            logger.warn("Could not archive legacy config {}", legacy.getFileName(), exception);
        }
    }
}
