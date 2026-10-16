package com.mawlee.cointcore.config;

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
 * File: {@code config/cointcore/playtime-reward.json}.
 */
public final class PlaytimeRewardConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "playtime-reward.json";

    private static PlaytimeRewardParse.Parsed settings = PlaytimeRewardParse.parse(null);

    private PlaytimeRewardConfig() {
    }

    public static PlaytimeRewardParse.Parsed get() {
        return settings;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        PlaytimeRewardParse.Parsed loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info("Reloaded playtime-reward config from {}", getConfigPath());
        return true;
    }

    private static void apply(PlaytimeRewardParse.Parsed loaded) {
        settings = loaded != null ? loaded : PlaytimeRewardParse.parse(null);
    }

    private static PlaytimeRewardParse.Parsed loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                PlaytimeRewardParse.FileData defaults = PlaytimeRewardParse.FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    PlaytimeRewardParse.gson().toJson(defaults, writer);
                }
                LOGGER.info("Created default playtime-reward config at {}", path);
                return PlaytimeRewardParse.parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                PlaytimeRewardParse.FileData data = PlaytimeRewardParse.gson()
                        .fromJson(reader, PlaytimeRewardParse.FileData.class);
                PlaytimeRewardParse.Parsed parsed = PlaytimeRewardParse.parse(data);
                if (!reloading) {
                    LOGGER.info("Loaded playtime-reward config from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load playtime-reward config from {}", path, exception);
            return reloading ? null : PlaytimeRewardParse.parse(null);
        }
    }
}
