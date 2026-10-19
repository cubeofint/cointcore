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
 * Server config for recursive / burst damage cancellation.
 *
 * <p>Does not special-case training dummies: infestation XP farms stay intact
 * until a real recursion or per-tick hit cap is exceeded.
 */
public final class DamageGuardConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static int maxDepth = 8;
    private static int maxHitsPerTargetPerTick = 20;
    private static boolean notifyAdmins = true;
    private static int logCooldownSeconds = 10;

    private DamageGuardConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getMaxDepth() {
        return maxDepth;
    }

    public static int getMaxHitsPerTargetPerTick() {
        return maxHitsPerTargetPerTick;
    }

    public static boolean isNotifyAdmins() {
        return notifyAdmins;
    }

    public static int getLogCooldownSeconds() {
        return logCooldownSeconds;
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
                "Reloaded damage guard config (enabled={}, max_depth={}, max_hits={})",
                enabled,
                maxDepth,
                maxHitsPerTargetPerTick
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        maxDepth = loaded.maxDepth();
        maxHitsPerTargetPerTick = loaded.maxHitsPerTargetPerTick();
        notifyAdmins = loaded.notifyAdmins();
        logCooldownSeconds = loaded.logCooldownSeconds();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default damage guard config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load damage guard config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading damage guard config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(
                data.enabled,
                clampInt(data.maxDepth, 1, 64, 8),
                clampInt(data.maxHitsPerTargetPerTick, 1, 10_000, 20),
                data.notifyAdmins,
                clampInt(data.logCooldownSeconds, 1, 3600, 10)
        );
    }

    private static int clampInt(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("damage-guard.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.maxDepth = 8;
        data.maxHitsPerTargetPerTick = 20;
        data.notifyAdmins = true;
        data.logCooldownSeconds = 10;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int maxDepth,
            int maxHitsPerTargetPerTick,
            boolean notifyAdmins,
            int logCooldownSeconds
    ) {
    }

    @SuppressWarnings("unused")
    static final class FileData {
        @SerializedName("enabled")
        private boolean enabled = true;

        @SerializedName("max_depth")
        int maxDepth = 8;

        @SerializedName("max_hits_per_target_per_tick")
        int maxHitsPerTargetPerTick = 20;

        @SerializedName("notify_admins")
        private boolean notifyAdmins = true;

        @SerializedName("log_cooldown_seconds")
        int logCooldownSeconds = 10;
    }
}
