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
 * Starter kit backed by an FTB Essentials kit name (contents + FTB cooldown live in KitManager).
 */
public final class StarterKitConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Settings settings = Settings.defaults();

    private StarterKitConfig() {
    }

    public static Settings get() {
        return settings;
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
                LOGGER.info("Created default starter kit config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded starter kit config (enabled={}, kit={}, cooldown={}s, firstJoin={})",
                        loaded.settings.enabled(),
                        loaded.settings.kitName(),
                        loaded.settings.cooldownSeconds(),
                        loaded.settings.firstJoinEnabled()
                );
                if (reloading) {
                    LOGGER.info("Reloaded starter kit config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load starter kit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading starter kit config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        settings = loaded.settings;
    }

    private static LoadedConfig parse(FileData data) {
        String kitName = data.kitName == null || data.kitName.isBlank() ? "start" : data.kitName.trim().toLowerCase();
        long cooldown = data.cooldownSeconds != null ? Math.max(0L, data.cooldownSeconds) : 86_400L;
        boolean enabled = data.enabled == null || data.enabled;
        boolean firstJoin = data.firstJoinEnabled == null || data.firstJoinEnabled;
        return new LoadedConfig(new Settings(enabled, kitName, cooldown, firstJoin));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("starter-kit.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.kitName = "start";
        data.cooldownSeconds = 86_400L;
        data.firstJoinEnabled = true;
        return data;
    }

    public record Settings(
            boolean enabled,
            String kitName,
            long cooldownSeconds,
            boolean firstJoinEnabled
    ) {
        public static Settings defaults() {
            return new Settings(true, "start", 86_400L, true);
        }
    }

    private record LoadedConfig(Settings settings) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("kitName")
        private String kitName;

        /** FTB kit cooldown in seconds (applied when saving kit from inventory / sync). */
        @SerializedName("cooldownSeconds")
        private Long cooldownSeconds;

        @SerializedName("firstJoinEnabled")
        private Boolean firstJoinEnabled;
    }
}
