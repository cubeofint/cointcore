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
 * When enabled, explosions never destroy blocks or change terrain.
 * TNT player damage is controlled separately and is off by default.
 */
public final class ExplosionTerrainConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static boolean tntPlayerDamage;
    private static boolean allowSpawnerDetonation = true;

    private ExplosionTerrainConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isTntPlayerDamageEnabled() {
        return tntPlayerDamage;
    }

    public static boolean isSpawnerDetonationAllowed() {
        return allowSpawnerDetonation;
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
        LOGGER.info("Reloaded explosion terrain config (enabled={}, tnt_player_damage={}, allow_spawner_detonation={})", enabled, tntPlayerDamage, allowSpawnerDetonation);
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        tntPlayerDamage = loaded.tntPlayerDamage();
        allowSpawnerDetonation = loaded.allowSpawnerDetonation();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default explosion terrain config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load explosion terrain config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading explosion terrain config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        boolean tntDamage = Boolean.TRUE.equals(data.tntPlayerDamage);
        boolean spawners = data.allowSpawnerDetonation == null || data.allowSpawnerDetonation;
        return new LoadedConfig(on, tntDamage, spawners);
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.tntPlayerDamage = false;
        data.allowSpawnerDetonation = true;
        return data;
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("explosion-terrain.json");
    }

    private record LoadedConfig(boolean enabled, boolean tntPlayerDamage, boolean allowSpawnerDetonation) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("tnt_player_damage")
        private Boolean tntPlayerDamage;

        @SerializedName("allow_spawner_detonation")
        private Boolean allowSpawnerDetonation;
    }
}
