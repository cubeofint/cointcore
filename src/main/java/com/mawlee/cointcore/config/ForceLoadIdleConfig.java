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
 * Shorter FTB Chunks offline force-load window for players without {@code cointcore.forceload.extended}.
 * FTB's own {@code max_idle_days_before_unforce} stays the long (donor) window; teams idle longer than
 * {@code idle_hours} with no extended member get their force-loaded chunks released by cointcore.
 *
 * <p>File: {@code config/cointcore/forceload-idle.json}.
 */
public final class ForceLoadIdleConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24 * 365;
    private static final int MIN_CHECK_MINUTES = 1;
    private static final int MAX_CHECK_MINUTES = 24 * 60;

    private static boolean enabled = true;
    private static int idleHours = 24;
    private static int checkIntervalMinutes = 10;

    private ForceLoadIdleConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getIdleHours() {
        return idleHours;
    }

    public static int getCheckIntervalMinutes() {
        return checkIntervalMinutes;
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
        LOGGER.info("Reloaded force-load idle config (enabled={}, idleHours={}, checkIntervalMinutes={})",
                enabled, idleHours, checkIntervalMinutes);
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        idleHours = loaded.idleHours();
        checkIntervalMinutes = loaded.checkIntervalMinutes();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = new FileData();
                save(defaults, path);
                LOGGER.info("Created default force-load idle config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : new FileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load force-load idle config from {}", configPath(), exception);
            return reloading ? null : parse(new FileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading force-load idle config from {}", configPath(), exception);
            return reloading ? null : parse(new FileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(
                data.enabled,
                Math.max(MIN_HOURS, Math.min(MAX_HOURS, data.idleHours)),
                Math.max(MIN_CHECK_MINUTES, Math.min(MAX_CHECK_MINUTES, data.checkIntervalMinutes))
        );
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("forceload-idle.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private record LoadedConfig(boolean enabled, int idleHours, int checkIntervalMinutes) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        boolean enabled = true;

        @SerializedName("idle_hours")
        int idleHours = 24;

        @SerializedName("check_interval_minutes")
        int checkIntervalMinutes = 10;
    }
}
