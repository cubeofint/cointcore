package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.vote.VoteType;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoteConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static VoteSettings dayVote = VoteSettings.defaults(30, 300, 60);
    private static VoteSettings clearWeatherVote = VoteSettings.defaults(30, 300, 60);
    private static int sleepPercentage = 30;
    private static boolean environmentCooldownEnabled = true;
    private static int environmentCooldownSeconds = 300;

    private VoteConfig() {
    }

    public static VoteSettings getSettings(VoteType type) {
        // if/else instead of switch: avoids synthetic VoteConfig$1 switch-map,
        // which can NoClassDefFoundError if the jar is replaced on a live JVM.
        if (type == VoteType.DAY) {
            return dayVote;
        }
        if (type == VoteType.CLEAR_WEATHER) {
            return clearWeatherVote;
        }
        throw new IllegalArgumentException("Unknown vote type: " + type);
    }

    public static int getSleepPercentage() {
        return sleepPercentage;
    }

    public static boolean isEnvironmentCooldownEnabled() {
        return environmentCooldownEnabled;
    }

    public static int getEnvironmentCooldownSeconds() {
        return environmentCooldownSeconds;
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
                LOGGER.info("Created default vote config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded vote config (sleep={}%, envCooldownEnabled={}, envCooldown={}s)",
                        loaded.sleepPercentage,
                        loaded.environmentCooldownEnabled,
                        loaded.environmentCooldownSeconds
                );
                if (reloading) {
                    LOGGER.info("Reloaded vote config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load vote config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading vote config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        dayVote = loaded.dayVote;
        clearWeatherVote = loaded.clearWeatherVote;
        sleepPercentage = loaded.sleepPercentage;
        environmentCooldownEnabled = loaded.environmentCooldownEnabled;
        environmentCooldownSeconds = loaded.environmentCooldownSeconds;
    }

    private static LoadedConfig parse(FileData data) {
        VoteSettings dayDefaults = VoteSettings.defaults(30, 300, 60);
        VoteSettings weatherDefaults = VoteSettings.defaults(30, 300, 60);
        VoteSettings day = normalizeSettings(data.dayVote, dayDefaults);
        VoteSettings weather = normalizeSettings(data.clearWeatherVote, weatherDefaults);

        boolean envEnabled = data.environmentCooldownEnabled == null || data.environmentCooldownEnabled;
        int envSeconds;
        if (data.environmentCooldownSeconds != null) {
            envSeconds = Math.max(0, data.environmentCooldownSeconds);
        } else {
            // Backward compatible default: reuse the larger of the old per-vote cooldowns.
            envSeconds = Math.max(day.cooldownSeconds(), weather.cooldownSeconds());
        }

        return new LoadedConfig(
                day,
                weather,
                clampPercentage(data.sleepPercentage != null ? data.sleepPercentage : 30),
                envEnabled,
                envSeconds
        );
    }

    private static VoteSettings normalizeSettings(VoteSettingsFile source, VoteSettings defaults) {
        if (source == null) {
            return defaults;
        }

        int percentage = source.requiredPercentage != null && source.requiredPercentage > 0
                ? source.requiredPercentage
                : legacyVotesToPercentage(source.requiredVotes, defaults.requiredPercentage());

        return new VoteSettings(
                clampVotePercentage(percentage),
                Math.max(0, source.cooldownSeconds != null ? source.cooldownSeconds : defaults.cooldownSeconds()),
                Math.max(10, source.durationSeconds != null ? source.durationSeconds : defaults.durationSeconds())
        );
    }

    private static int legacyVotesToPercentage(Integer requiredVotes, int fallback) {
        if (requiredVotes == null || requiredVotes <= 0) {
            return fallback;
        }
        return Math.min(100, requiredVotes * 10);
    }

    private static int clampPercentage(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int clampVotePercentage(int value) {
        return Math.max(1, Math.min(100, value));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("votes.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.dayVote = VoteSettingsFile.defaults(30, 300, 60);
        data.clearWeatherVote = VoteSettingsFile.defaults(30, 300, 60);
        data.sleepPercentage = 30;
        data.environmentCooldownEnabled = true;
        data.environmentCooldownSeconds = 300;
        return data;
    }

    public record VoteSettings(int requiredPercentage, int cooldownSeconds, int durationSeconds) {
        public static VoteSettings defaults(int requiredPercentage, int cooldownSeconds, int durationSeconds) {
            return new VoteSettings(requiredPercentage, cooldownSeconds, durationSeconds);
        }
    }

    private static final class VoteSettingsFile {
        @SerializedName("requiredPercentage")
        private Integer requiredPercentage;

        @SerializedName("requiredVotes")
        private Integer requiredVotes;

        @SerializedName("cooldownSeconds")
        private Integer cooldownSeconds;

        @SerializedName("durationSeconds")
        private Integer durationSeconds;

        private static VoteSettingsFile defaults(int requiredPercentage, int cooldownSeconds, int durationSeconds) {
            VoteSettingsFile settings = new VoteSettingsFile();
            settings.requiredPercentage = requiredPercentage;
            settings.cooldownSeconds = cooldownSeconds;
            settings.durationSeconds = durationSeconds;
            return settings;
        }
    }

    private record LoadedConfig(
            VoteSettings dayVote,
            VoteSettings clearWeatherVote,
            int sleepPercentage,
            boolean environmentCooldownEnabled,
            int environmentCooldownSeconds
    ) {
    }

    private static final class FileData {
        @SerializedName("dayVote")
        private VoteSettingsFile dayVote;

        @SerializedName("clearWeatherVote")
        private VoteSettingsFile clearWeatherVote;

        @SerializedName("sleepPercentage")
        private Integer sleepPercentage;

        @SerializedName("environmentCooldownEnabled")
        private Boolean environmentCooldownEnabled;

        @SerializedName("environmentCooldownSeconds")
        private Integer environmentCooldownSeconds;
    }
}
