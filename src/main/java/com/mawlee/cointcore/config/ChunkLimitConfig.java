package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChunkLimitConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static Map<ResourceLocation, Integer> blockLimits = Map.of();
    private static Map<ResourceLocation, Integer> entityLimits = Map.of();

    private ChunkLimitConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Map<ResourceLocation, Integer> getBlockLimits() {
        return blockLimits;
    }

    public static Map<ResourceLocation, Integer> getEntityLimits() {
        return entityLimits;
    }

    public static Integer getBlockLimit(ResourceLocation id) {
        return blockLimits.get(id);
    }

    public static Integer getEntityLimit(ResourceLocation id) {
        return entityLimits.get(id);
    }

    public static Path getConfigPath() {
        return configPath();
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
        return true;
    }

    public static boolean setEnabled(boolean value) {
        enabled = value;
        return saveCurrentState();
    }

    public static boolean setBlockLimit(ResourceLocation id, int limit) {
        if (limit < 0) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(blockLimits);
        updated.put(id, limit);
        blockLimits = Map.copyOf(updated);
        return saveCurrentState();
    }

    public static boolean removeBlockLimit(ResourceLocation id) {
        if (!blockLimits.containsKey(id)) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(blockLimits);
        updated.remove(id);
        blockLimits = Map.copyOf(updated);
        return saveCurrentState();
    }

    public static boolean setEntityLimit(ResourceLocation id, int limit) {
        if (limit < 0) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(entityLimits);
        updated.put(id, limit);
        entityLimits = Map.copyOf(updated);
        return saveCurrentState();
    }

    public static boolean removeEntityLimit(ResourceLocation id) {
        if (!entityLimits.containsKey(id)) {
            return false;
        }
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(entityLimits);
        updated.remove(id);
        entityLimits = Map.copyOf(updated);
        return saveCurrentState();
    }

    private static boolean saveCurrentState() {
        try {
            FileData data = new FileData();
            data.enabled = enabled;
            data.blockLimits = toStringMap(blockLimits);
            data.entityLimits = toStringMap(entityLimits);
            save(data, configPath());
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to save chunk limit config to {}", configPath(), exception);
            return false;
        }
    }

    private static Map<String, Integer> toStringMap(Map<ResourceLocation, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : source.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default chunk limit config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded chunk limit config (enabled: {}, {} block limits, {} entity limits)",
                        loaded.enabled,
                        loaded.blockLimits.size(),
                        loaded.entityLimits.size()
                );
                if (reloading) {
                    LOGGER.info("Reloaded chunk limit config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load chunk limit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading chunk limit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled;
        blockLimits = loaded.blockLimits;
        entityLimits = loaded.entityLimits;
    }

    private static LoadedConfig parse(FileData data) {
        boolean active = data.enabled != null ? data.enabled : true;
        return new LoadedConfig(
                active,
                parseLimitMap(data.blockLimits, true),
                parseLimitMap(data.entityLimits, false)
        );
    }

    private static Map<ResourceLocation, Integer> parseLimitMap(Map<String, Integer> source, boolean blocks) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> limits = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey().trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid chunk limit id: {}", entry.getKey());
                continue;
            }
            if (blocks) {
                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    LOGGER.warn("Unknown chunk limit block: {}", id);
                }
            } else if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                LOGGER.warn("Unknown chunk limit entity type: {}", id);
            }
            limits.put(id, entry.getValue());
        }
        return Collections.unmodifiableMap(limits);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("chunk-limits.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.blockLimits = new LinkedHashMap<>();
        data.entityLimits = new LinkedHashMap<>();
        return data;
    }

    public static boolean hasBlockLimit(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && blockLimits.containsKey(id);
    }

    public static boolean hasEntityLimit(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && entityLimits.containsKey(id);
    }

    private record LoadedConfig(
            boolean enabled,
            Map<ResourceLocation, Integer> blockLimits,
            Map<ResourceLocation, Integer> entityLimits
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("blockLimits")
        private Map<String, Integer> blockLimits = new HashMap<>();

        @SerializedName("entityLimits")
        private Map<String, Integer> entityLimits = new HashMap<>();
    }
}
