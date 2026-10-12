package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.questrank.QuestRankSettings;
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
 * Maps FTB Quests ids (hex) to LuckPerms groups. Rank completions are per-player;
 * {@code resetRankQuestTeamProgress} (default true) clears that quest on the FTB team
 * after crediting so a teammate can complete it themselves.
 */
public final class QuestRankConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "quest-ranks.json";
    public static final int DEFAULT_SWEEP_INTERVAL_SECONDS = QuestRankSettings.DEFAULT_SWEEP_INTERVAL_SECONDS;

    private static QuestRankSettings settings = QuestRankSettings.defaults();

    private QuestRankConfig() {
    }

    public static QuestRankSettings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        QuestRankSettings loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
                LOGGER.info(
                        "Reloaded quest-rank config (enabled={}, mappings={}, sweep={}s, resetTeamProgress={}) from {}",
                        settings.enabled(),
                        settings.table().size(),
                        settings.sweepIntervalSeconds(),
                        settings.resetRankQuestTeamProgress(),
                        getConfigPath()
                );
        return true;
    }

    private static void apply(QuestRankSettings loaded) {
        settings = loaded != null ? loaded : QuestRankSettings.defaults();
    }

    private static QuestRankSettings loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ConfigMergeSupport.GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default quest-rank config at {}", path);
                return fromFile(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = ConfigMergeSupport.GSON.fromJson(reader, FileData.class);
                QuestRankSettings parsed = fromFile(data);
                if (!reloading) {
                    LOGGER.info(
                            "Loaded quest-rank config (enabled={}, mappings={}, sweep={}s, resetTeamProgress={}) from {}",
                            parsed.enabled(),
                            parsed.table().size(),
                            parsed.sweepIntervalSeconds(),
                            parsed.resetRankQuestTeamProgress(),
                            path
                    );
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load quest-rank config from {}", path, exception);
            return reloading ? null : QuestRankSettings.defaults();
        }
    }

    static QuestRankSettings fromFile(FileData data) {
        if (data == null) {
            return QuestRankSettings.defaults();
        }
        return QuestRankSettings.from(
                data.enabled,
                data.sweepIntervalSeconds,
                data.resetRankQuestTeamProgress,
                data.quests
        );
    }

    static final class FileData {
        Boolean enabled;
        @SerializedName("sweepIntervalSeconds")
        Integer sweepIntervalSeconds;
        @SerializedName("resetRankQuestTeamProgress")
        Boolean resetRankQuestTeamProgress;
        Map<String, String> quests;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.sweepIntervalSeconds = DEFAULT_SWEEP_INTERVAL_SECONDS;
            data.resetRankQuestTeamProgress = true;
            data.quests = defaultQuests();
            return data;
        }

        static Map<String, String> defaultQuests() {
            return new LinkedHashMap<>(QuestRankTable.DEFAULT_QUESTS);
        }
    }
}
