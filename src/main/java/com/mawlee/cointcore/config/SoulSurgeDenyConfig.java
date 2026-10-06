package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Editable Soul Surge acceleration denylist ({@code config/cointcore/soul-surge-deny.json}).
 * Defaults are copied from the bundled resource on first create — not hardcoded in Java.
 */
public final class SoulSurgeDenyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/soul-surge-deny.json";

    private static Set<ResourceLocation> denyBlockIds = Set.of();
    private static Set<String> denyBlockNamespaces = Set.of();
    private static List<TagKey<Block>> denyBlockTags = List.of();
    private static final IdentityHashMap<Block, Boolean> DENY_CACHE = new IdentityHashMap<>();

    private SoulSurgeDenyConfig() {
    }

    public static boolean isDenied(BlockState state) {
        Block block = state.getBlock();
        synchronized (DENY_CACHE) {
            Boolean cached = DENY_CACHE.get(block);
            if (cached != null) {
                return cached;
            }
            boolean denied = computeDenied(block, state);
            DENY_CACHE.put(block, denied);
            return denied;
        }
    }

    private static boolean computeDenied(Block block, BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            if (denyBlockIds.contains(id)) {
                return true;
            }
            if (!denyBlockNamespaces.isEmpty() && denyBlockNamespaces.contains(id.getNamespace())) {
                return true;
            }
        }
        for (TagKey<Block> tag : denyBlockTags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static int denyIdCount() {
        return denyBlockIds.size();
    }

    public static int denyNamespaceCount() {
        return denyBlockNamespaces.size();
    }

    public static int denyTagCount() {
        return denyBlockTags.size();
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
                "Reloaded soul surge deny config (denyIds={} denyNamespaces={} denyTags={})",
                denyBlockIds.size(),
                denyBlockNamespaces.size(),
                denyBlockTags.size()
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        denyBlockIds = loaded.denyBlockIds();
        denyBlockNamespaces = loaded.denyBlockNamespaces();
        denyBlockTags = loaded.denyBlockTags();
        synchronized (DENY_CACHE) {
            DENY_CACHE.clear();
        }
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created default soul surge deny config at {}", path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : new FileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge deny config from {}", configPath(), exception);
            return reloading ? null : parse(readBundledDefaults());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge deny config from {}", configPath(), exception);
            return reloading ? null : parse(readBundledDefaults());
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        FileData migrated = tryMigrateFromPerfConfig();
        if (migrated != null) {
            save(migrated, path);
            LOGGER.info("Migrated soul surge deny lists from soul-surge-perf.json into {}", path);
            return;
        }
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing empty deny lists", DEFAULT_RESOURCE);
        save(new FileData(), path);
    }

    /**
     * One-shot migration: older installs stored deny lists inside {@code soul-surge-perf.json}.
     */
    private static FileData tryMigrateFromPerfConfig() {
        Path perfPath = FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-perf.json");
        if (!Files.exists(perfPath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(perfPath, StandardCharsets.UTF_8)) {
            LegacyPerfDenyData legacy = GSON.fromJson(reader, LegacyPerfDenyData.class);
            if (legacy == null) {
                return null;
            }
            boolean hasIds = legacy.denyBlockIds != null && !legacy.denyBlockIds.isEmpty();
            boolean hasNs = legacy.denyBlockNamespaces != null && !legacy.denyBlockNamespaces.isEmpty();
            boolean hasTags = legacy.denyBlockTags != null && !legacy.denyBlockTags.isEmpty();
            if (!hasIds && !hasNs && !hasTags) {
                return null;
            }
            FileData migrated = new FileData();
            migrated.denyBlockIds = legacy.denyBlockIds;
            migrated.denyBlockNamespaces = legacy.denyBlockNamespaces;
            migrated.denyBlockTags = legacy.denyBlockTags;
            return migrated;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Could not migrate deny lists from {}", perfPath, exception);
            return null;
        }
    }

    private static FileData readBundledDefaults() {
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled soul surge deny defaults", exception);
            return new FileData();
        }
    }

    private static LoadedConfig parse(FileData data) {
        Set<ResourceLocation> ids = new HashSet<>();
        List<String> rawIds = data.denyBlockIds != null ? data.denyBlockIds : List.of();
        for (String raw : rawIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(raw.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid soul surge deny block id: {}", raw);
                continue;
            }
            ids.add(id);
        }

        Set<String> namespaces = new HashSet<>();
        List<String> rawNamespaces = data.denyBlockNamespaces != null ? data.denyBlockNamespaces : List.of();
        for (String raw : rawNamespaces) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String ns = raw.trim().toLowerCase(Locale.ROOT);
            if (ns.startsWith("#")) {
                LOGGER.warn("Skipping invalid soul surge deny namespace (looks like a tag): {}", raw);
                continue;
            }
            if (ns.indexOf(':') >= 0) {
                LOGGER.warn("Skipping invalid soul surge deny namespace (use mod id only): {}", raw);
                continue;
            }
            namespaces.add(ns);
        }

        List<TagKey<Block>> tags = new ArrayList<>();
        List<String> rawTags = data.denyBlockTags != null ? data.denyBlockTags : List.of();
        for (String raw : rawTags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.startsWith("#")) {
                trimmed = trimmed.substring(1);
            }
            ResourceLocation tagId = ResourceLocation.tryParse(trimmed);
            if (tagId == null) {
                LOGGER.warn("Skipping invalid soul surge deny block tag: {}", raw);
                continue;
            }
            tags.add(TagKey.create(Registries.BLOCK, tagId));
        }

        return new LoadedConfig(
                Collections.unmodifiableSet(ids),
                Collections.unmodifiableSet(namespaces),
                List.copyOf(tags)
        );
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-deny.json");
    }

    private record LoadedConfig(
            Set<ResourceLocation> denyBlockIds,
            Set<String> denyBlockNamespaces,
            List<TagKey<Block>> denyBlockTags
    ) {
    }

    private static final class FileData {
        @SerializedName("denyBlockIds")
        private List<String> denyBlockIds;

        @SerializedName("denyBlockNamespaces")
        private List<String> denyBlockNamespaces;

        @SerializedName("denyBlockTags")
        private List<String> denyBlockTags;
    }

    /** Fields formerly embedded in {@code soul-surge-perf.json}. */
    private static final class LegacyPerfDenyData {
        @SerializedName("denyBlockIds")
        private List<String> denyBlockIds;

        @SerializedName("denyBlockNamespaces")
        private List<String> denyBlockNamespaces;

        @SerializedName("denyBlockTags")
        private List<String> denyBlockTags;
    }
}
