package com.mawlee.cointcore.config;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.privilege.ExpAddUnit;
import com.mawlee.cointcore.privilege.PrivilegeCommandId;
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
 * File: {@code config/cointcore/privilege-commands.json}.
 */
public final class PrivilegeCommandsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "privilege-commands.json";

    private static PrivilegeCommandsParse.Parsed settings = PrivilegeCommandsParse.parse(null);

    private PrivilegeCommandsConfig() {
    }

    public static PrivilegeCommandsParse.Parsed get() {
        return settings;
    }

    public static int cooldownSeconds(PrivilegeCommandId id) {
        return settings.cooldownSeconds(id);
    }

    public static int expAddMaxAmount() {
        return settings.expAddMaxAmount();
    }

    public static ExpAddUnit expAddUnit() {
        return settings.expAddUnit();
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        PrivilegeCommandsParse.Parsed loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info("Reloaded privilege-commands config from {}", getConfigPath());
        return true;
    }

    private static void apply(PrivilegeCommandsParse.Parsed loaded) {
        settings = loaded != null ? loaded : PrivilegeCommandsParse.parse(null);
    }

    private static PrivilegeCommandsParse.Parsed loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                PrivilegeCommandsParse.FileData defaults = PrivilegeCommandsParse.FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    PrivilegeCommandsParse.gson().toJson(defaults, writer);
                }
                LOGGER.info("Created default privilege-commands config at {}", path);
                return PrivilegeCommandsParse.parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                PrivilegeCommandsParse.FileData data = PrivilegeCommandsParse.gson()
                        .fromJson(reader, PrivilegeCommandsParse.FileData.class);
                PrivilegeCommandsParse.Parsed parsed = PrivilegeCommandsParse.parse(data);
                if (!reloading) {
                    LOGGER.info("Loaded privilege-commands config from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load privilege-commands config from {}", path, exception);
            return reloading ? null : PrivilegeCommandsParse.parse(null);
        }
    }
}
