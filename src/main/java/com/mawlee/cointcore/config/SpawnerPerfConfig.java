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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spawner TPS opts ({@code config/cointcore/spawner-perf.json}).
 * Stops failed-spawn thrashing, caches nearby scans, floors spawn delay / spawnCount.
 */
public final class SpawnerPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/spawner-perf.json";

    private static boolean enabled = true;
    private static boolean forceDelayOnFailedSpawn = true;
    private static int minFailCooldownTicks = 40;
    private static boolean cacheNearbyEntityScans = true;
    private static int minSpawnDelayFloor = 40;
    private static int maxSpawnCount = 4;

    private SpawnerPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean forceDelayOnFailedSpawn() {
        return forceDelayOnFailedSpawn;
    }

    public static int minFailCooldownTicks() {
        return minFailCooldownTicks;
    }

    public static boolean cacheNearbyEntityScans() {
        return cacheNearbyEntityScans;
    }

    public static int minSpawnDelayFloor() {
        return minSpawnDelayFloor;
    }

    public static int maxSpawnCount() {
        return maxSpawnCount;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        FileData data = loadFromDisk(true);
        if (data == null) {
            return false;
        }
        apply(data);
        LOGGER.info(
                "Reloaded spawner perf config (enabled={} forceDelay={} minFailCooldown={} cacheNearby={} delayFloor={} maxSpawnCount={})",
                enabled,
                forceDelayOnFailedSpawn,
                minFailCooldownTicks,
                cacheNearbyEntityScans,
                minSpawnDelayFloor,
                maxSpawnCount
        );
        return true;
    }

    private static void apply(FileData data) {
        enabled = data.enabled == null || data.enabled;
        forceDelayOnFailedSpawn = data.forceDelayOnFailedSpawn == null || data.forceDelayOnFailedSpawn;
        cacheNearbyEntityScans = data.cacheNearbyEntityScans == null || data.cacheNearbyEntityScans;
        int cooldown = data.minFailCooldownTicks != null ? data.minFailCooldownTicks : 40;
        minFailCooldownTicks = Math.max(0, Math.min(cooldown, 200));
        int floor = data.minSpawnDelayFloor != null ? data.minSpawnDelayFloor : 40;
        minSpawnDelayFloor = Math.max(0, Math.min(floor, 800));
        int count = data.maxSpawnCount != null ? data.maxSpawnCount : 4;
        maxSpawnCount = Math.max(1, Math.min(count, 16));
    }

    private static FileData loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created default spawner perf config at {}", path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load spawner perf config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading spawner perf config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = SpawnerPerfConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing defaults", DEFAULT_RESOURCE);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(new FileData(), writer);
        }
    }

    private static FileData readBundledDefaults() {
        try (InputStream in = SpawnerPerfConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled spawner perf defaults", exception);
            return new FileData();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("spawner-perf.json");
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled = true;

        @SerializedName("forceDelayOnFailedSpawn")
        private Boolean forceDelayOnFailedSpawn = true;

        @SerializedName("minFailCooldownTicks")
        private Integer minFailCooldownTicks = 40;

        @SerializedName("cacheNearbyEntityScans")
        private Boolean cacheNearbyEntityScans = true;

        @SerializedName("minSpawnDelayFloor")
        private Integer minSpawnDelayFloor = 40;

        @SerializedName("maxSpawnCount")
        private Integer maxSpawnCount = 4;
    }
}
