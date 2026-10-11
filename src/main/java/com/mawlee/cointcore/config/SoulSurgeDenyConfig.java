package com.mawlee.cointcore.config;

import com.google.gson.JsonSyntaxException;
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

/**
 * Deprecated. Soul Surge no longer consults a denylist — each surge accelerates
 * only its attached block entity. This class still loads
 * {@code config/cointcore/soul-surge-deny.json} so leftover files (including
 * {@code denyBlockIds} / {@code denyBlockTags}) do not crash reload.
 */
public final class SoulSurgeDenyConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
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

    private static boolean loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created deprecated soul surge deny config stub at {}", path);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SoulSurgeDenyParse.FileData data = SoulSurgeDenyParse.gson()
                        .fromJson(reader, SoulSurgeDenyParse.FileData.class);
                SoulSurgeDenyParse.parse(data);
                return true;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                SoulSurgeDenyParse.parse(readBundledDefaults());
            }
            return !reloading;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge deny config from {}", configPath(), exception);
            if (!reloading) {
                SoulSurgeDenyParse.parse(readBundledDefaults());
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
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            SoulSurgeDenyParse.gson().toJson(SoulSurgeDenyParse.FileData.stub(), writer);
        }
    }

    private static SoulSurgeDenyParse.FileData readBundledDefaults() {
        try (InputStream in = SoulSurgeDenyConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new SoulSurgeDenyParse.FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                SoulSurgeDenyParse.FileData data = SoulSurgeDenyParse.gson()
                        .fromJson(reader, SoulSurgeDenyParse.FileData.class);
                return data != null ? data : new SoulSurgeDenyParse.FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled soul surge deny defaults", exception);
            return new SoulSurgeDenyParse.FileData();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-deny.json");
    }
}
