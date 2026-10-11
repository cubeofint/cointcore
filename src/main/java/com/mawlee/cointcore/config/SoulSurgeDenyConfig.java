package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
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
import java.util.List;

/**
 * Deprecated. Soul Surge no longer consults a denylist — each surge accelerates
 * only its attached block entity. This class still loads
 * {@code config/cointcore/soul-surge-deny.json} so leftover files (including
 * {@code denyBlockIds} / {@code denyBlockTags}) do not crash reload.
 */
public final class SoulSurgeDenyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/soul-surge-deny.json";

    private SoulSurgeDenyConfig() {
    }

    /**
     * Always {@code false}. Kept so leftover callers compile; Soul Surge does
     * not use this.
     */
    public static boolean isDenied(BlockState state) {
        return false;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        loadFromDisk(false);
    }

    public static boolean reload() {
        boolean ok = loadFromDisk(true);
        if (ok) {
            LOGGER.info("Reloaded soul-surge-deny.json (deprecated, unused)");
        }
        return ok;
    }

    /**
     * Parses JSON without touching disk. Used by tests and the disk loader.
     */
    static Parsed parseJson(String json) {
        FileData data = GSON.fromJson(json, FileData.class);
        return parse(data != null ? data : new FileData());
    }

    private static boolean loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created deprecated soul surge deny config stub at {}", path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                parse(data != null ? data : new FileData());
                return true;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                parse(readBundledDefaults());
            }
            return !reloading;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                parse(readBundledDefaults());
            }
            return !reloading;
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing deprecated stub", DEFAULT_RESOURCE);
        FileData stub = new FileData();
        stub.deprecated = true;
        stub.unused = true;
        stub.denyBlockIds = List.of();
        stub.denyBlockNamespaces = List.of();
        stub.denyBlockTags = List.of();
        save(stub, path);
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

    private static Parsed parse(FileData data) {
        List<String> ids = data.denyBlockIds != null ? List.copyOf(data.denyBlockIds) : List.of();
        List<String> namespaces = data.denyBlockNamespaces != null ? List.copyOf(data.denyBlockNamespaces) : List.of();
        List<String> tags = data.denyBlockTags != null ? List.copyOf(data.denyBlockTags) : List.of();
        return new Parsed(
                Boolean.TRUE.equals(data.deprecated) || Boolean.TRUE.equals(data.unused),
                ids,
                namespaces,
                tags
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

    record Parsed(
            boolean deprecated,
            List<String> denyBlockIds,
            List<String> denyBlockNamespaces,
            List<String> denyBlockTags
    ) {
    }

    private static final class FileData {
        @SerializedName("deprecated")
        private Boolean deprecated;

        @SerializedName("unused")
        private Boolean unused;

        @SerializedName("denyBlockIds")
        private List<String> denyBlockIds;

        @SerializedName("denyBlockNamespaces")
        private List<String> denyBlockNamespaces;

        @SerializedName("denyBlockTags")
        private List<String> denyBlockTags;
    }
}
