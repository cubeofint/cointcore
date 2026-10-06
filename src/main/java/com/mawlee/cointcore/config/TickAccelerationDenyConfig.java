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
import net.minecraft.world.level.block.SpawnerBlock;
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
 * Editable tick-acceleration denylist ({@code config/cointcore/tick-acceleration-deny.json}).
 * Used by Time Wand (and Soul Surge gate). Defaults copied from bundled resource on first create.
 */
public final class TickAccelerationDenyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/tick-acceleration-deny.json";

    private static boolean enabled = true;
    private static boolean denyVanillaSpawners = true;
    private static boolean blockFakePlayers = true;
    private static List<String> pathTokens = List.of();
    private static Set<ResourceLocation> denyBlockIds = Set.of();
    private static Set<String> denyBlockNamespaces = Set.of();
    private static List<TagKey<Block>> denyBlockTags = List.of();
    private static final IdentityHashMap<Block, Boolean> DENY_CACHE = new IdentityHashMap<>();

    private TickAccelerationDenyConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean blockFakePlayers() {
        return blockFakePlayers;
    }

    public static boolean isDenied(BlockState state) {
        if (!enabled || state == null) {
            return false;
        }
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
        if (denyVanillaSpawners && block instanceof SpawnerBlock) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            if (denyBlockIds.contains(id)) {
                return true;
            }
            if (!denyBlockNamespaces.isEmpty() && denyBlockNamespaces.contains(id.getNamespace())) {
                return true;
            }
            if (pathLooksDangerous(id.getPath())) {
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

    private static boolean pathLooksDangerous(String path) {
        if (path == null || path.isEmpty() || pathTokens.isEmpty()) {
            return false;
        }
        for (String token : pathTokens) {
            if (containsToken(path, token)) {
                return true;
            }
        }
        return false;
    }

    /** Token match so {@code embossed_*} does not match {@code boss}. */
    static boolean containsToken(String path, String token) {
        int from = 0;
        while (from <= path.length() - token.length()) {
            int i = path.indexOf(token, from);
            if (i < 0) {
                return false;
            }
            boolean leftOk = i == 0 || !Character.isLetterOrDigit(path.charAt(i - 1));
            int end = i + token.length();
            boolean rightOk = end >= path.length() || !Character.isLetterOrDigit(path.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = i + 1;
        }
        return false;
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
        LOGGER.info(
                "Reloaded tick acceleration deny config (enabled={} ids={} namespaces={} tags={} pathTokens={})",
                enabled,
                denyBlockIds.size(),
                denyBlockNamespaces.size(),
                denyBlockTags.size(),
                pathTokens.size()
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        denyVanillaSpawners = loaded.denyVanillaSpawners();
        blockFakePlayers = loaded.blockFakePlayers();
        pathTokens = loaded.pathTokens();
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
                LOGGER.info("Created default tick acceleration deny config at {}", path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                if (data == null) {
                    data = new FileData();
                }
                if (mergeMissingDefaults(data)) {
                    save(data, path);
                    LOGGER.info("Merged new tick-acceleration deny defaults into {}", path);
                }
                return parse(data);
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load tick acceleration deny config from {}", configPath(), exception);
            return reloading ? null : parse(readBundledDefaults());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading tick acceleration deny config from {}", configPath(), exception);
            return reloading ? null : parse(readBundledDefaults());
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = TickAccelerationDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing empty deny lists", DEFAULT_RESOURCE);
        save(new FileData(), path);
    }

    private static FileData readBundledDefaults() {
        try (InputStream in = TickAccelerationDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled tick acceleration deny defaults", exception);
            return new FileData();
        }
    }

    /**
     * Union bundled deny lists into an existing on-disk config so jar updates apply
     * without wiping admin customizations. Returns true if the file should be rewritten.
     */
    private static boolean mergeMissingDefaults(FileData disk) {
        FileData bundled = readBundledDefaults();
        boolean changed = false;
        changed |= mergeStringList(disk::getPathTokens, disk::setPathTokens, bundled.pathTokens);
        changed |= mergeStringList(disk::getDenyBlockIds, disk::setDenyBlockIds, bundled.denyBlockIds);
        changed |= mergeStringList(disk::getDenyBlockNamespaces, disk::setDenyBlockNamespaces, bundled.denyBlockNamespaces);
        changed |= mergeStringList(disk::getDenyBlockTags, disk::setDenyBlockTags, bundled.denyBlockTags);
        return changed;
    }

    private static boolean mergeStringList(
            java.util.function.Supplier<List<String>> getter,
            java.util.function.Consumer<List<String>> setter,
            List<String> extras
    ) {
        if (extras == null || extras.isEmpty()) {
            return false;
        }
        List<String> current = getter.get();
        if (current == null) {
            current = new ArrayList<>();
        } else {
            current = new ArrayList<>(current);
        }
        Set<String> seen = new HashSet<>();
        for (String value : current) {
            if (value != null) {
                seen.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        boolean changed = false;
        for (String raw : extras) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                current.add(raw.trim());
                changed = true;
            }
        }
        if (changed) {
            setter.accept(current);
        }
        return changed;
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        boolean spawners = data.denyVanillaSpawners == null || data.denyVanillaSpawners;
        boolean fake = data.blockFakePlayers == null || data.blockFakePlayers;

        List<String> tokens = new ArrayList<>();
        List<String> rawTokens = data.pathTokens != null ? data.pathTokens : List.of();
        for (String raw : rawTokens) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            tokens.add(raw.trim().toLowerCase(Locale.ROOT));
        }

        Set<ResourceLocation> ids = new HashSet<>();
        List<String> rawIds = data.denyBlockIds != null ? data.denyBlockIds : List.of();
        for (String raw : rawIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(raw.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid tick-accel deny block id: {}", raw);
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
            if (ns.startsWith("#") || ns.indexOf(':') >= 0) {
                LOGGER.warn("Skipping invalid tick-accel deny namespace: {}", raw);
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
                LOGGER.warn("Skipping invalid tick-accel deny block tag: {}", raw);
                continue;
            }
            tags.add(TagKey.create(Registries.BLOCK, tagId));
        }

        return new LoadedConfig(
                on,
                spawners,
                fake,
                List.copyOf(tokens),
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
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("tick-acceleration-deny.json");
    }

    private record LoadedConfig(
            boolean enabled,
            boolean denyVanillaSpawners,
            boolean blockFakePlayers,
            List<String> pathTokens,
            Set<ResourceLocation> denyBlockIds,
            Set<String> denyBlockNamespaces,
            List<TagKey<Block>> denyBlockTags
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("denyVanillaSpawners")
        private Boolean denyVanillaSpawners;

        @SerializedName("blockFakePlayers")
        private Boolean blockFakePlayers;

        @SerializedName("pathTokens")
        private List<String> pathTokens;

        @SerializedName("denyBlockIds")
        private List<String> denyBlockIds;

        @SerializedName("denyBlockNamespaces")
        private List<String> denyBlockNamespaces;

        @SerializedName("denyBlockTags")
        private List<String> denyBlockTags;

        private List<String> getPathTokens() {
            return pathTokens;
        }

        private void setPathTokens(List<String> pathTokens) {
            this.pathTokens = pathTokens;
        }

        private List<String> getDenyBlockIds() {
            return denyBlockIds;
        }

        private void setDenyBlockIds(List<String> denyBlockIds) {
            this.denyBlockIds = denyBlockIds;
        }

        private List<String> getDenyBlockNamespaces() {
            return denyBlockNamespaces;
        }

        private void setDenyBlockNamespaces(List<String> denyBlockNamespaces) {
            this.denyBlockNamespaces = denyBlockNamespaces;
        }

        private List<String> getDenyBlockTags() {
            return denyBlockTags;
        }

        private void setDenyBlockTags(List<String> denyBlockTags) {
            this.denyBlockTags = denyBlockTags;
        }
    }
}
