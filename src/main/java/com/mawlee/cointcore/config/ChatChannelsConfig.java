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
 * Local / global / trade chat channels (port of the CointCoreGTO chat).
 *
 * <p>File: {@code config/cointcore/chat-channels.json}. Replaces Re-LPChatPrefix; remove that mod when enabling.
 */
public final class ChatChannelsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Settings settings = Settings.defaults();

    private ChatChannelsConfig() {
    }

    public static Settings get() {
        return settings;
    }

    public static boolean isEnabled() {
        return settings.enabled();
    }

    public static double getLocalRadius() {
        return settings.localRadius();
    }

    public static void load() {
        settings = loadFromDisk(false);
    }

    public static boolean reload() {
        Settings loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        settings = loaded;
        LOGGER.info("Reloaded chat channels config (enabled={}, localRadius={})", settings.enabled(), settings.localRadius());
        return true;
    }

    private static Settings loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = new FileData();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default chat channels config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : new FileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load chat channels config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading chat channels config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        }
    }

    private static Settings parse(FileData data) {
        return new Settings(
                data.enabled,
                Math.max(1.0D, Math.min(100_000.0D, data.localRadius)),
                nonNull(data.localPrefix, "&a[L] "),
                nonNull(data.globalPrefix, "&6[G] "),
                nonNull(data.tradePrefix, "&b[$] "),
                nonNull(data.privatePrefix, "&d[PM] "),
                nonNull(data.globalChar, "!"),
                nonNull(data.tradeChar, "$"),
                Math.max(0, Math.min(3600, data.tradeCooldownSeconds)),
                data.advancementsLocal,
                data.nobodyNearbyHint
        );
    }

    private static String nonNull(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("chat-channels.json");
    }

    public record Settings(
            boolean enabled,
            double localRadius,
            String localPrefix,
            String globalPrefix,
            String tradePrefix,
            String privatePrefix,
            String globalChar,
            String tradeChar,
            int tradeCooldownSeconds,
            boolean advancementsLocal,
            boolean nobodyNearbyHint
    ) {
        static Settings defaults() {
            return parse(new FileData());
        }
    }

    private static final class FileData {
        @SerializedName("enabled")
        boolean enabled = true;

        @SerializedName("local_radius")
        double localRadius = 100.0D;

        @SerializedName("local_prefix")
        String localPrefix = "&a[L] ";

        @SerializedName("global_prefix")
        String globalPrefix = "&6[G] ";

        @SerializedName("trade_prefix")
        String tradePrefix = "&b[$] ";

        @SerializedName("private_prefix")
        String privatePrefix = "&d[PM] ";

        /** Message starting with this goes to global chat. Empty disables. */
        @SerializedName("global_char")
        String globalChar = "!";

        /** Message starting with this goes to trade chat. Empty disables. */
        @SerializedName("trade_char")
        String tradeChar = "$";

        @SerializedName("trade_cooldown_seconds")
        int tradeCooldownSeconds = 10;

        /** Advancement / challenge announcements only reach players within the local radius. */
        @SerializedName("advancements_local")
        boolean advancementsLocal = true;

        /** Action-bar hint when a local message reached nobody else. */
        @SerializedName("nobody_nearby_hint")
        boolean nobodyNearbyHint = true;
    }
}
