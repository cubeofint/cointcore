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
 * File: {@code config/cointcore/ftbranks-luckperms-bridge.json}
 *
 * <p>{@code ftbranksLuckPermsBridge} defaults to {@code false}: FTB Ranks behaviour is unchanged
 * until an admin explicitly enables the LuckPerms fallback.
 */
public final class FtbRanksLuckPermsBridgeConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ftbranks-luckperms-bridge.json";

    private static boolean enabled = false;
    private static boolean debug = false;

    private FtbRanksLuckPermsBridgeConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
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
                "Reloaded FTB Ranks LuckPerms bridge config (ftbranksLuckPermsBridge={}, debug={})",
                enabled,
                debug
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        debug = loaded.debug();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default FTB Ranks LuckPerms bridge config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded FTB Ranks LuckPerms bridge config (ftbranksLuckPermsBridge={}, debug={})",
                        loaded.enabled(),
                        loaded.debug()
                );
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load FTB Ranks LuckPerms bridge config from {}", getConfigPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading FTB Ranks LuckPerms bridge config from {}", getConfigPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean parsedEnabled = Boolean.TRUE.equals(data.ftbranksLuckPermsBridge);
        boolean parsedDebug = Boolean.TRUE.equals(data.ftbranksLuckPermsBridgeDebug);
        return new LoadedConfig(parsedEnabled, parsedDebug);
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.ftbranksLuckPermsBridge = false;
        data.ftbranksLuckPermsBridgeDebug = false;
        return data;
    }

    private record LoadedConfig(boolean enabled, boolean debug) {
    }

    private static final class FileData {
        @SerializedName("ftbranksLuckPermsBridge")
        Boolean ftbranksLuckPermsBridge;

        @SerializedName("ftbranksLuckPermsBridgeDebug")
        Boolean ftbranksLuckPermsBridgeDebug;
    }
}
