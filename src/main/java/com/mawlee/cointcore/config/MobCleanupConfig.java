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
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MobCleanupConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Set<ResourceLocation> removeEntityTypes = Set.of();
    private static Set<ResourceLocation> excludeEntityTypes = Set.of();
    private static boolean useHostileFallback = true;

    private MobCleanupConfig() {
    }

    public static Set<ResourceLocation> getRemoveEntityTypes() {
        return removeEntityTypes;
    }

    public static Set<ResourceLocation> getExcludeEntityTypes() {
        return excludeEntityTypes;
    }

    public static boolean useHostileFallback() {
        return useHostileFallback;
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

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default mob cleanup config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded mob cleanup config ({} remove types, {} exclude types)",
                        loaded.removeEntityTypes.size(),
                        loaded.excludeEntityTypes.size()
                );
                if (reloading) {
                    LOGGER.info("Reloaded mob cleanup config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load mob cleanup config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading mob cleanup config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        removeEntityTypes = loaded.removeEntityTypes;
        excludeEntityTypes = loaded.excludeEntityTypes;
        useHostileFallback = loaded.useHostileFallback;
    }

    private static LoadedConfig parse(FileData data) {
        Set<ResourceLocation> removeTypes = parseEntityTypeIds(data.removeEntityTypes, "removeEntityTypes");
        Set<ResourceLocation> excludeTypes = parseEntityTypeIds(data.excludeEntityTypes, "excludeEntityTypes");
        parseLegacyNameExceptions(data.nameExceptions, excludeTypes);

        boolean fallback = data.useHostileFallback != null ? data.useHostileFallback : true;
        return new LoadedConfig(Set.copyOf(removeTypes), Set.copyOf(excludeTypes), fallback);
    }

    private static Set<ResourceLocation> parseEntityTypeIds(List<String> rawTypes, String fieldName) {
        Set<ResourceLocation> types = new HashSet<>();
        if (rawTypes == null) {
            return types;
        }
        for (String rawType : rawTypes) {
            if (rawType == null || rawType.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(rawType.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid mob cleanup {} id: {}", fieldName, rawType);
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                LOGGER.warn("Unknown mob cleanup entity type in {}: {}", fieldName, id);
            }
            types.add(id);
        }
        return types;
    }

    private static void parseLegacyNameExceptions(List<String> legacyNames, Set<ResourceLocation> excludeTypes) {
        if (legacyNames == null) {
            return;
        }
        for (String rawName : legacyNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String trimmed = rawName.trim();
            ResourceLocation id = ResourceLocation.tryParse(trimmed);
            if (id == null) {
                LOGGER.warn(
                        "mob-cleanup.json: ignoring deprecated nameExceptions entry '{}' (use excludeEntityTypes with entity id, e.g. minecraft:warden)",
                        trimmed
                );
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                LOGGER.warn(
                        "mob-cleanup.json: nameExceptions entry '{}' is not a known entity type; move it to excludeEntityTypes if valid",
                        trimmed
                );
            }
            if (excludeTypes.add(id)) {
                LOGGER.warn(
                        "mob-cleanup.json: migrated nameExceptions entry '{}' to excludeEntityTypes (nameExceptions is deprecated)",
                        trimmed
                );
            }
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("mob-cleanup.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.removeEntityTypes = new ArrayList<>(List.of(
                "minecraft:zombie",
                "minecraft:skeleton",
                "minecraft:creeper",
                "minecraft:spider",
                "minecraft:enderman"
        ));
        data.excludeEntityTypes = new ArrayList<>(List.of(
                "minecraft:wither",
                "minecraft:ender_dragon",
                "minecraft:warden"
        ));
        data.useHostileFallback = true;
        return data;
    }

    public static boolean matchesConfiguredType(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && removeEntityTypes.contains(id);
    }

    public static boolean matchesExcludedType(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && excludeEntityTypes.contains(id);
    }

    private record LoadedConfig(
            Set<ResourceLocation> removeEntityTypes,
            Set<ResourceLocation> excludeEntityTypes,
            boolean useHostileFallback
    ) {
    }

    private static final class FileData {
        @SerializedName("removeEntityTypes")
        private List<String> removeEntityTypes = new ArrayList<>();

        @SerializedName("excludeEntityTypes")
        private List<String> excludeEntityTypes = new ArrayList<>();

        /** @deprecated use {@link #excludeEntityTypes} with entity ids such as {@code minecraft:warden} */
        @Deprecated
        @SerializedName("nameExceptions")
        private List<String> nameExceptions = new ArrayList<>();

        @SerializedName("useHostileFallback")
        private Boolean useHostileFallback;
    }
}
