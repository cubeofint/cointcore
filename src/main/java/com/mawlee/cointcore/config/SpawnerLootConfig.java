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
 * Virtual farm-spawner loot ({@code config/cointcore/spawner-loot.json}).
 * When enabled, machine/vanilla farm spawners roll loot into adjacent storage
 * instead of adding mobs. Disabled restores the original spawn behaviour.
 */
public final class SpawnerLootConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/spawner-loot.json";

    private static boolean enabled = true;
    private static boolean skipFinalizeMobSpawn = true;
    private static boolean affixDropsEnabled = true;
    private static float affixChanceMultiplier = 1.0F;
    private static int affixMaxPerMinute = 8;
    private static boolean eliteDrops = false;

    private SpawnerLootConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean skipFinalizeMobSpawn() {
        return enabled && skipFinalizeMobSpawn;
    }

    public static boolean affixDropsEnabled() {
        return enabled && affixDropsEnabled;
    }

    public static float affixChanceMultiplier() {
        return affixChanceMultiplier;
    }

    public static int affixMaxPerMinute() {
        return affixMaxPerMinute;
    }

    public static boolean eliteDrops() {
        return eliteDrops;
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
                "Reloaded spawner loot config (enabled={} skipFinalize={} affix={} mult={} max/min={} elite={})",
                enabled,
                skipFinalizeMobSpawn,
                affixDropsEnabled,
                affixChanceMultiplier,
                affixMaxPerMinute,
                eliteDrops
        );
        return true;
    }

    private static void apply(FileData data) {
        Settings settings = Settings.from(data);
        enabled = settings.enabled;
        skipFinalizeMobSpawn = settings.skipFinalizeMobSpawn;
        affixDropsEnabled = settings.affixDropsEnabled;
        affixChanceMultiplier = settings.affixChanceMultiplier;
        affixMaxPerMinute = settings.affixMaxPerMinute;
        eliteDrops = settings.eliteDrops;
    }

    private static FileData loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created default spawner loot config at {}", path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load spawner loot config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading spawner loot config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = SpawnerLootConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
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
        try (InputStream in = SpawnerLootConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled spawner loot defaults", exception);
            return new FileData();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("spawner-loot.json");
    }

    public static final class Settings {
        public final boolean enabled;
        public final boolean skipFinalizeMobSpawn;
        public final boolean affixDropsEnabled;
        public final float affixChanceMultiplier;
        public final int affixMaxPerMinute;
        public final boolean eliteDrops;

        private Settings(
                boolean enabled,
                boolean skipFinalizeMobSpawn,
                boolean affixDropsEnabled,
                float affixChanceMultiplier,
                int affixMaxPerMinute,
                boolean eliteDrops
        ) {
            this.enabled = enabled;
            this.skipFinalizeMobSpawn = skipFinalizeMobSpawn;
            this.affixDropsEnabled = affixDropsEnabled;
            this.affixChanceMultiplier = affixChanceMultiplier;
            this.affixMaxPerMinute = affixMaxPerMinute;
            this.eliteDrops = eliteDrops;
        }

        public static Settings from(FileData data) {
            FileData source = data != null ? data : new FileData();
            boolean on = source.enabled == null || source.enabled;
            boolean skipFinalize = source.skipFinalizeMobSpawn == null || source.skipFinalizeMobSpawn;
            AffixFileData affix = source.apotheosisAffixDrops != null ? source.apotheosisAffixDrops : new AffixFileData();
            boolean affixOn = affix.enabled == null || affix.enabled;
            float multiplier = affix.chanceMultiplier != null ? affix.chanceMultiplier : 1.0F;
            if (multiplier < 0.0F) {
                multiplier = 0.0F;
            }
            if (multiplier > 64.0F) {
                multiplier = 64.0F;
            }
            int maxPerMinute = affix.maxPerMinute != null ? affix.maxPerMinute : 8;
            maxPerMinute = Math.max(0, Math.min(maxPerMinute, 10_000));
            boolean elite = affix.eliteDrops != null && affix.eliteDrops;
            return new Settings(on, skipFinalize, affixOn, multiplier, maxPerMinute, elite);
        }
    }

    public static final class FileData {
        @SerializedName("enabled")
        public Boolean enabled = true;

        @SerializedName("skip_finalize_mob_spawn")
        public Boolean skipFinalizeMobSpawn = true;

        @SerializedName("apotheosis_affix_drops")
        public AffixFileData apotheosisAffixDrops = new AffixFileData();
    }

    public static final class AffixFileData {
        @SerializedName("enabled")
        public Boolean enabled = true;

        @SerializedName("chance_multiplier")
        public Float chanceMultiplier = 1.0F;

        @SerializedName("max_per_minute")
        public Integer maxPerMinute = 8;

        @SerializedName("elite_drops")
        public Boolean eliteDrops = false;
    }
}
