package com.mawlee.cointcore.config;

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
 * Optional HTTP forwarder for the currency-movement outbox.
 * Disabled by default. Leave {@code authorization} empty unless the site issues a real token.
 * Never configure an API that overwrites absolute site balances.
 */
public final class CurrencyMovementConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Settings settings = Settings.disabled();

    private CurrencyMovementConfig() {
    }

    public static Settings get() {
        return settings;
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
        return true;
    }

    private static void apply(Settings loaded) {
        settings = loaded != null ? loaded : Settings.disabled();
        com.mawlee.cointcore.shop.CurrencyMovementService.refreshSink();
    }

    private static Settings loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ConfigMergeSupport.GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default currency-movement config at {}", path);
                return Settings.disabled();
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = ConfigMergeSupport.GSON.fromJson(reader, FileData.class);
                Settings parsed = Settings.from(data);
                if (reloading) {
                    LOGGER.info("Reloaded currency-movement config from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load currency-movement config from {}", path, exception);
            return reloading ? null : Settings.disabled();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("currency-movement.json");
    }

    public record Settings(boolean enabled, String endpointUrl, String authorizationHeader,
                           boolean siteQueueEnabled, int siteQueuePollSeconds) {
        static Settings disabled() {
            return new Settings(false, "", "", false, 5);
        }

        static Settings from(FileData data) {
            if (data == null) {
                return disabled();
            }
            return new Settings(
                    data.enabled,
                    data.endpointUrl != null ? data.endpointUrl.trim() : "",
                    data.authorization != null ? data.authorization.trim() : "",
                    data.siteQueueEnabled,
                    Math.max(1, Math.min(300, data.siteQueuePollSeconds <= 0 ? 5 : data.siteQueuePollSeconds))
            );
        }
    }

    static final class FileData {
        boolean enabled;
        @SerializedName("endpoint_url")
        String endpointUrl;
        String authorization;
        /** Pull site&lt;-&gt;server transfers through AzLink. Off by default. */
        @SerializedName("site_queue_enabled")
        boolean siteQueueEnabled;
        @SerializedName("site_queue_poll_seconds")
        int siteQueuePollSeconds = 5;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = false;
            data.endpointUrl = "";
            data.authorization = "";
            data.siteQueueEnabled = false;
            data.siteQueuePollSeconds = 5;
            return data;
        }
    }
}
