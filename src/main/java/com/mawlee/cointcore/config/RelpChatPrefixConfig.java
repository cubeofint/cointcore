package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mirror of Re-LPChatPrefix settings from {@code config/relpchatprefix/config.json}.
 * Local chat delivery and radius are owned by Re-LPChatPrefix; CointCore reads the same file for chat spy.
 */
public final class RelpChatPrefixConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_GLOBAL_PREFIX = "!";
    private static final double DEFAULT_LOCAL_RADIUS = 100.0D;

    private static String globalPrefix = DEFAULT_GLOBAL_PREFIX;
    private static double localRadius = DEFAULT_LOCAL_RADIUS;

    private RelpChatPrefixConfig() {
    }

    public static String getGlobalPrefix() {
        return globalPrefix;
    }

    public static double getLocalRadius() {
        return localRadius;
    }

    public static void load() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            globalPrefix = DEFAULT_GLOBAL_PREFIX;
            localRadius = DEFAULT_LOCAL_RADIUS;
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FileData data = GSON.fromJson(reader, FileData.class);
            if (data == null) {
                applyDefaults();
                return;
            }

            globalPrefix = data.globalPrefix != null && !data.globalPrefix.isEmpty()
                    ? data.globalPrefix
                    : DEFAULT_GLOBAL_PREFIX;
            localRadius = data.localRadius > 0.0D ? data.localRadius : DEFAULT_LOCAL_RADIUS;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read Re-LPChatPrefix config from {}, using defaults", path, exception);
            applyDefaults();
        }
    }

    private static void applyDefaults() {
        globalPrefix = DEFAULT_GLOBAL_PREFIX;
        localRadius = DEFAULT_LOCAL_RADIUS;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("relpchatprefix").resolve("config.json");
    }

    private static final class FileData {
        @SerializedName("globalPrefix")
        private String globalPrefix = DEFAULT_GLOBAL_PREFIX;

        @SerializedName("localRadius")
        private double localRadius = DEFAULT_LOCAL_RADIUS;
    }
}
