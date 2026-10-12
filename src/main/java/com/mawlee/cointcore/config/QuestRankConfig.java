package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.questrank.QuestRankTable;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File: {@code config/cointcore/quest-ranks.json}.
 * Maps FTB Quests ids (hex) to LuckPerms groups.
 */
public final class QuestRankConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "quest-ranks.json";
    public static final int DEFAULT_SWEEP_INTERVAL_SECONDS = 60;

    private static Settings settings = Settings.defaults();

    private QuestRankConfig() {
    }

    public static Settings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        Settings loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info(
                "Reloaded quest-rank config (enabled={}, mappings={}, sweep={}s) from {}",
                settings.enabled(),
                settings.table().size(),
                settings.sweepIntervalSeconds(),
                getConfigPath()
        );
        return true;
    }

    private static void apply(Settings loaded) {
        settings = loaded != null ? loaded : Settings.defaults();
    }

    private static Settings loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ConfigMergeSupport.GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default quest-rank config at {}", path);
                return Settings.from(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = ConfigMergeSupport.GSON.fromJson(reader, FileData.class);
                Settings parsed = Settings.from(data);
                if (!reloading) {
                    LOGGER.info(
                            "Loaded quest-rank config (enabled={}, mappings={}, sweep={}s) from {}",
                            parsed.enabled(),
                            parsed.table().size(),
                            parsed.sweepIntervalSeconds(),
                            path
                    );
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load quest-rank config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        }
    }

    public record Settings(boolean enabled, int sweepIntervalSeconds, QuestRankTable table) {
        static Settings defaults() {
            return from(FileData.defaults());
        }

        static Settings from(FileData data) {
            if (data == null) {
                data = FileData.defaults();
            }
            Map<String, String> quests = data.quests != null ? data.quests : FileData.defaultQuests();
            return new Settings(
                    data.enabled == null || data.enabled,
                    clamp(data.sweepIntervalSeconds, 5, 3600, DEFAULT_SWEEP_INTERVAL_SECONDS),
                    QuestRankTable.parse(quests)
            );
        }
    }

    private static int clamp(Integer value, int min, int max, int fallback) {
        if (value == null || value < min) {
            return fallback;
        }
        return Math.min(max, value);
    }

    static final class FileData {
        Boolean enabled;
        @SerializedName("sweepIntervalSeconds")
        Integer sweepIntervalSeconds;
        Map<String, String> quests;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.sweepIntervalSeconds = DEFAULT_SWEEP_INTERVAL_SECONDS;
            data.quests = defaultQuests();
            return data;
        }

        static Map<String, String> defaultQuests() {
            return new LinkedHashMap<>(QuestRankTable.DEFAULT_QUESTS);
        }
    }
}
