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

/**
 * Limits vanilla natural mob spawn volume around players:
 * Chebyshev chunk radius + vertical window (±Y).
 */
public final class NaturalSpawnConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int MIN_CHUNK_RADIUS = 0;
    private static final int MAX_CHUNK_RADIUS = 32;
    private static final int MIN_VERTICAL = 0;
    private static final int MAX_VERTICAL = 512;

    private static boolean enabled = true;
    private static int maxChunkRadius = 3;
    private static int maxVerticalBlocks = 20;

    private NaturalSpawnConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Chebyshev distance in chunks from player chunk to spawn chunk (inclusive). */
    public static int getMaxChunkRadius() {
        return maxChunkRadius;
    }

    /** Absolute Y delta from nearest qualifying player. */
    public static int getMaxVerticalBlocks() {
        return maxVerticalBlocks;
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        LoadedConfig loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info(
                "Reloaded natural spawn config (enabled={}, maxChunkRadius={}, maxVerticalBlocks={})",
                enabled,
                maxChunkRadius,
                maxVerticalBlocks
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        maxChunkRadius = loaded.maxChunkRadius();
        maxVerticalBlocks = loaded.maxVerticalBlocks();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default natural spawn config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded natural spawn config (enabled={}, maxChunkRadius={}, maxVerticalBlocks={})",
                        loaded.enabled(),
                        loaded.maxChunkRadius(),
                        loaded.maxVerticalBlocks()
                );
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load natural spawn config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading natural spawn config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        int chunkRadius = clamp(data.maxChunkRadius, 3, MIN_CHUNK_RADIUS, MAX_CHUNK_RADIUS);
        int vertical = clamp(data.maxVerticalBlocks, 20, MIN_VERTICAL, MAX_VERTICAL);
        return new LoadedConfig(on, chunkRadius, vertical);
    }

    private static int clamp(Integer value, int fallback, int min, int max) {
        int resolved = value != null ? value : fallback;
        return Math.max(min, Math.min(max, resolved));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("natural-spawn.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.maxChunkRadius = 3;
        data.maxVerticalBlocks = 20;
        return data;
    }

    private record LoadedConfig(boolean enabled, int maxChunkRadius, int maxVerticalBlocks) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("max_chunk_radius")
        Integer maxChunkRadius;

        @SerializedName("max_vertical_blocks")
        Integer maxVerticalBlocks;
    }
}
