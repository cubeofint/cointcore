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
 * AFK timers are armed on strong activity / join: mark after {@code mark_after_seconds},
 * kick after {@code kick_after_seconds} (absolute from last strong activity).
 *
 * <p>Weak actions (inventory clicks, swing, GUI heartbeat) never reset timers.
 *
 * <p>File: {@code config/cointcore/afk.json}
 */
public final class AfkConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 86_400;
    private static final double MIN_LOOK_DEGREES = 0.1D;
    private static final double MAX_LOOK_DEGREES = 90.0D;

    private static final int DEFAULT_MARK_AFTER = 60;
    private static final int DEFAULT_KICK_AFTER = 300;
    private static final int DEFAULT_SUSPECT_THRESHOLD = 10;
    private static final int DEFAULT_SUSPECT_NOTIFY_COOLDOWN = 120;

    private static boolean enabled = true;
    private static boolean guiHeartbeatEnabled = true;
    private static int markAfterSeconds = DEFAULT_MARK_AFTER;
    private static int kickAfterSeconds = DEFAULT_KICK_AFTER;
    private static double lookThresholdDegrees = 1.0D;
    private static int suspectWeakActionsThreshold = DEFAULT_SUSPECT_THRESHOLD;
    private static int suspectNotifyCooldownSeconds = DEFAULT_SUSPECT_NOTIFY_COOLDOWN;

    private AfkConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isGuiHeartbeatEnabled() {
        return guiHeartbeatEnabled;
    }

    public static int getMarkAfterSeconds() {
        return markAfterSeconds;
    }

    public static int getKickAfterSeconds() {
        return kickAfterSeconds;
    }

    public static double getLookThresholdDegrees() {
        return lookThresholdDegrees;
    }

    public static int getSuspectWeakActionsThreshold() {
        return suspectWeakActionsThreshold;
    }

    public static int getSuspectNotifyCooldownSeconds() {
        return suspectNotifyCooldownSeconds;
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
                "Reloaded AFK config (enabled={}, guiHeartbeat={}, mark={}s, kick={}s, lookThreshold={}, suspectThreshold={}, suspectCooldown={}s)",
                enabled,
                guiHeartbeatEnabled,
                markAfterSeconds,
                kickAfterSeconds,
                lookThresholdDegrees,
                suspectWeakActionsThreshold,
                suspectNotifyCooldownSeconds
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        guiHeartbeatEnabled = loaded.guiHeartbeatEnabled();
        markAfterSeconds = loaded.markAfterSeconds();
        kickAfterSeconds = loaded.kickAfterSeconds();
        lookThresholdDegrees = loaded.lookThresholdDegrees();
        suspectWeakActionsThreshold = loaded.suspectWeakActionsThreshold();
        suspectNotifyCooldownSeconds = loaded.suspectNotifyCooldownSeconds();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default AFK config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded AFK config (enabled={}, guiHeartbeat={}, mark={}s, kick={}s, lookThreshold={}, suspectThreshold={}, suspectCooldown={}s)",
                        loaded.enabled(),
                        loaded.guiHeartbeatEnabled(),
                        loaded.markAfterSeconds(),
                        loaded.kickAfterSeconds(),
                        loaded.lookThresholdDegrees(),
                        loaded.suspectWeakActionsThreshold(),
                        loaded.suspectNotifyCooldownSeconds()
                );
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load AFK config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading AFK config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        boolean guiHeartbeat = data.guiHeartbeatEnabled == null || data.guiHeartbeatEnabled;
        int mark = clampSeconds(data.markAfterSeconds, DEFAULT_MARK_AFTER);
        int kick = clampSeconds(data.kickAfterSeconds, DEFAULT_KICK_AFTER);
        if (kick < mark) {
            kick = mark;
        }
        double look = data.lookThresholdDegrees != null ? data.lookThresholdDegrees : 1.0D;
        look = Math.max(MIN_LOOK_DEGREES, Math.min(MAX_LOOK_DEGREES, look));
        int suspectThreshold = data.suspectWeakActionsThreshold != null
                ? Math.max(1, Math.min(10_000, data.suspectWeakActionsThreshold))
                : DEFAULT_SUSPECT_THRESHOLD;
        int suspectCooldown = clampSeconds(data.suspectNotifyCooldownSeconds, DEFAULT_SUSPECT_NOTIFY_COOLDOWN);
        return new LoadedConfig(on, guiHeartbeat, mark, kick, look, suspectThreshold, suspectCooldown);
    }

    private static int clampSeconds(Integer value, int fallback) {
        int seconds = value != null ? value : fallback;
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("afk.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.guiHeartbeatEnabled = true;
        data.markAfterSeconds = DEFAULT_MARK_AFTER;
        data.kickAfterSeconds = DEFAULT_KICK_AFTER;
        data.lookThresholdDegrees = 1.0D;
        data.suspectWeakActionsThreshold = DEFAULT_SUSPECT_THRESHOLD;
        data.suspectNotifyCooldownSeconds = DEFAULT_SUSPECT_NOTIFY_COOLDOWN;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            boolean guiHeartbeatEnabled,
            int markAfterSeconds,
            int kickAfterSeconds,
            double lookThresholdDegrees,
            int suspectWeakActionsThreshold,
            int suspectNotifyCooldownSeconds
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("gui_heartbeat_enabled")
        Boolean guiHeartbeatEnabled;

        @SerializedName("mark_after_seconds")
        Integer markAfterSeconds;

        @SerializedName("kick_after_seconds")
        Integer kickAfterSeconds;

        @SerializedName("look_threshold_degrees")
        Double lookThresholdDegrees;

        @SerializedName("suspect_weak_actions_threshold")
        Integer suspectWeakActionsThreshold;

        @SerializedName("suspect_notify_cooldown_seconds")
        Integer suspectNotifyCooldownSeconds;

        /** Legacy field; ignored. Kept so old configs still parse. */
        @SerializedName("warn_after_seconds")
        @SuppressWarnings("unused")
        Integer warnAfterSeconds;
    }
}
