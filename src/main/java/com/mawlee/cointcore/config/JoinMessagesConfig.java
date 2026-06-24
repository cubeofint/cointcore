package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JoinMessagesConfig {
    public static final int LINE_COUNT = 6;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<String> lines = defaultLines();

    private JoinMessagesConfig() {
    }

    public static List<String> getLines() {
        return lines;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                save(defaultFileData(), path);
                lines = defaultLines();
                LOGGER.info("Created default join messages config at {}", path);
                return;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                lines = normalizeLines(data != null ? data.lines : null);
            }
            LOGGER.info("Loaded join messages config ({} lines)", lines.size());
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load join messages config from {}", configPath(), exception);
            lines = defaultLines();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading join messages config from {}", configPath(), exception);
            lines = defaultLines();
        }
    }

    public static boolean reload() {
        try {
            Path path = configPath();
            if (!Files.exists(path)) {
                save(defaultFileData(), path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                lines = normalizeLines(data != null ? data.lines : null);
            }
            LOGGER.info("Reloaded join messages config from {}", path);
            return true;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to reload join messages config from {}", configPath(), exception);
            return false;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while reloading join messages config from {}", configPath(), exception);
            return false;
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("join-messages.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.lines = new ArrayList<>(defaultLines());
        return data;
    }

    private static List<String> defaultLines() {
        return new ArrayList<>(Collections.nCopies(LINE_COUNT, ""));
    }

    private static List<String> normalizeLines(List<String> source) {
        List<String> normalized = new ArrayList<>(LINE_COUNT);
        if (source != null) {
            for (int index = 0; index < LINE_COUNT && index < source.size(); index++) {
                String line = source.get(index);
                normalized.add(line != null ? line : "");
            }
        }
        while (normalized.size() < LINE_COUNT) {
            normalized.add("");
        }
        return List.copyOf(normalized);
    }

    private static final class FileData {
        @SerializedName("lines")
        private List<String> lines = new ArrayList<>();
    }
}
