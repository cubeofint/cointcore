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
 * File: {@code config/cointcore/reserved-slots.json}.
 */
public final class ReservedSlotsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "reserved-slots.json";

    private static ReservedSlotsParse.Parsed settings = ReservedSlotsParse.parse(null);

    private ReservedSlotsConfig() {
    }

    public static ReservedSlotsParse.Parsed get() {
        return settings;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        ReservedSlotsParse.Parsed loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info("Reloaded reserved-slots config from {}", getConfigPath());
        return true;
    }

    private static void apply(ReservedSlotsParse.Parsed loaded) {
        settings = loaded != null ? loaded : ReservedSlotsParse.parse(null);
    }

    private static ReservedSlotsParse.Parsed loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                ReservedSlotsParse.FileData defaults = ReservedSlotsParse.FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ReservedSlotsParse.gson().toJson(defaults, writer);
                }
                LOGGER.info("Created default reserved-slots config at {}", path);
                return ReservedSlotsParse.parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ReservedSlotsParse.FileData data = ReservedSlotsParse.gson()
                        .fromJson(reader, ReservedSlotsParse.FileData.class);
                ReservedSlotsParse.Parsed parsed = ReservedSlotsParse.parse(data);
                if (!reloading) {
                    LOGGER.info("Loaded reserved-slots config from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load reserved-slots config from {}", path, exception);
            return reloading ? null : ReservedSlotsParse.parse(null);
        }
    }
}
