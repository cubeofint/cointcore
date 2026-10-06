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
 * Relays local chat and private messages to a Discord text channel.
 *
 * <p>File: {@code config/cointcore/chat-discord-relay.json}
 */
public final class ChatDiscordRelayConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_LOCAL = "[L] {player}: {message}";
    private static final String DEFAULT_PM = "[PM] {from} -> {to}: {message}";

    private static boolean enabled = false;
    private static String discordChannelId = "";
    private static boolean logLocalChat = true;
    private static boolean logPrivateMessages = true;
    private static String localFormat = DEFAULT_LOCAL;
    private static String pmFormat = DEFAULT_PM;

    private ChatDiscordRelayConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getDiscordChannelId() {
        return discordChannelId;
    }

    public static boolean isLogLocalChat() {
        return logLocalChat;
    }

    public static boolean isLogPrivateMessages() {
        return logPrivateMessages;
    }

    public static String getLocalFormat() {
        return localFormat;
    }

    public static String getPmFormat() {
        return pmFormat;
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
        LOGGER.info(
                "Reloaded chat Discord relay (enabled={}, channelId={}, local={}, pm={})",
                enabled,
                discordChannelId.isEmpty() ? "<empty>" : discordChannelId,
                logLocalChat,
                logPrivateMessages
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        discordChannelId = loaded.discordChannelId();
        logLocalChat = loaded.logLocalChat();
        logPrivateMessages = loaded.logPrivateMessages();
        localFormat = loaded.localFormat();
        pmFormat = loaded.pmFormat();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default chat Discord relay config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load chat Discord relay config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading chat Discord relay config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled != null && data.enabled;
        String channel = data.discordChannelId != null ? data.discordChannelId.trim() : "";
        boolean local = data.logLocalChat == null || data.logLocalChat;
        boolean pm = data.logPrivateMessages == null || data.logPrivateMessages;
        String localFmt = data.localFormat != null && !data.localFormat.isBlank() ? data.localFormat : DEFAULT_LOCAL;
        String pmFmt = data.pmFormat != null && !data.pmFormat.isBlank() ? data.pmFormat : DEFAULT_PM;
        return new LoadedConfig(on, channel, local, pm, localFmt, pmFmt);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("chat-discord-relay.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = false;
        data.discordChannelId = "";
        data.logLocalChat = true;
        data.logPrivateMessages = true;
        data.localFormat = DEFAULT_LOCAL;
        data.pmFormat = DEFAULT_PM;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            String discordChannelId,
            boolean logLocalChat,
            boolean logPrivateMessages,
            String localFormat,
            String pmFormat
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("discord_channel_id")
        String discordChannelId;

        @SerializedName("log_local_chat")
        Boolean logLocalChat;

        @SerializedName("log_private_messages")
        Boolean logPrivateMessages;

        @SerializedName("local_format")
        String localFormat;

        @SerializedName("pm_format")
        String pmFormat;
    }
}
