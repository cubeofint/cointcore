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
 * Off-thread stall detector. Samples a tick-start timestamp written by the server thread.
 */
public final class StallWatchdogConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static int warnAfterSeconds = 5;
    private static int dumpIntervalSeconds = 5;
    private static int maxDumps = 6;
    private static int notifyStaffAfterSeconds = 10;
    private static int breakLoopAfterSeconds = 15;
    private static boolean breakLoopEnabled = true;
    private static boolean discardOffendingEntity = false;
    private static int pollIntervalMs = 250;
    private static String dumpDir = "logs/cointcore-stalls";
    private static boolean notifyDiscord = true;

    private StallWatchdogConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getWarnAfterSeconds() {
        return warnAfterSeconds;
    }

    public static long getWarnAfterMs() {
        return warnAfterSeconds * 1000L;
    }

    public static int getDumpIntervalSeconds() {
        return dumpIntervalSeconds;
    }

    public static long getDumpIntervalMs() {
        return dumpIntervalSeconds * 1000L;
    }

    public static int getMaxDumps() {
        return maxDumps;
    }

    public static int getNotifyStaffAfterSeconds() {
        return notifyStaffAfterSeconds;
    }

    public static long getNotifyStaffAfterMs() {
        return notifyStaffAfterSeconds * 1000L;
    }

    public static int getBreakLoopAfterSeconds() {
        return breakLoopAfterSeconds;
    }

    public static long getBreakLoopAfterMs() {
        return breakLoopAfterSeconds * 1000L;
    }

    public static boolean isBreakLoopEnabled() {
        return breakLoopEnabled;
    }

    public static boolean isDiscardOffendingEntity() {
        return discardOffendingEntity;
    }

    public static int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public static String getDumpDir() {
        return dumpDir;
    }

    public static boolean isNotifyDiscord() {
        return notifyDiscord;
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
                "Reloaded stall watchdog config (enabled={}, warn_after={}s, break_loop_after={}s)",
                enabled,
                warnAfterSeconds,
                breakLoopAfterSeconds
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        warnAfterSeconds = loaded.warnAfterSeconds();
        dumpIntervalSeconds = loaded.dumpIntervalSeconds();
        maxDumps = loaded.maxDumps();
        notifyStaffAfterSeconds = loaded.notifyStaffAfterSeconds();
        breakLoopAfterSeconds = loaded.breakLoopAfterSeconds();
        breakLoopEnabled = loaded.breakLoopEnabled();
        discardOffendingEntity = loaded.discardOffendingEntity();
        pollIntervalMs = loaded.pollIntervalMs();
        dumpDir = loaded.dumpDir();
        notifyDiscord = loaded.notifyDiscord();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default stall watchdog config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load stall watchdog config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading stall watchdog config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        String dir = data.dumpDir == null || data.dumpDir.isBlank()
                ? "logs/cointcore-stalls"
                : data.dumpDir.trim();
        return new LoadedConfig(
                data.enabled,
                clampInt(data.warnAfterSeconds, 1, 600, 5),
                clampInt(data.dumpIntervalSeconds, 1, 600, 5),
                clampInt(data.maxDumps, 1, 50, 6),
                clampInt(data.notifyStaffAfterSeconds, 1, 600, 10),
                clampInt(data.breakLoopAfterSeconds, 1, 600, 15),
                data.breakLoopEnabled,
                data.discardOffendingEntity,
                clampInt(data.pollIntervalMs, 50, 5000, 250),
                dir,
                data.notifyDiscord
        );
    }

    private static int clampInt(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("stall-watchdog.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.warnAfterSeconds = 5;
        data.dumpIntervalSeconds = 5;
        data.maxDumps = 6;
        data.notifyStaffAfterSeconds = 10;
        data.breakLoopAfterSeconds = 15;
        data.breakLoopEnabled = true;
        data.discardOffendingEntity = false;
        data.pollIntervalMs = 250;
        data.dumpDir = "logs/cointcore-stalls";
        data.notifyDiscord = true;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int warnAfterSeconds,
            int dumpIntervalSeconds,
            int maxDumps,
            int notifyStaffAfterSeconds,
            int breakLoopAfterSeconds,
            boolean breakLoopEnabled,
            boolean discardOffendingEntity,
            int pollIntervalMs,
            String dumpDir,
            boolean notifyDiscord
    ) {
    }

    @SuppressWarnings("unused")
    static final class FileData {
        @SerializedName("enabled")
        private boolean enabled = true;

        @SerializedName("warn_after")
        int warnAfterSeconds = 5;

        @SerializedName("dump_interval")
        int dumpIntervalSeconds = 5;

        @SerializedName("max_dumps")
        int maxDumps = 6;

        @SerializedName("notify_staff_after")
        int notifyStaffAfterSeconds = 10;

        @SerializedName("break_loop_after")
        int breakLoopAfterSeconds = 15;

        @SerializedName("break_loop_enabled")
        private boolean breakLoopEnabled = true;

        @SerializedName("discard_offending_entity")
        private boolean discardOffendingEntity = false;

        @SerializedName("poll_interval_ms")
        int pollIntervalMs = 250;

        @SerializedName("dump_dir")
        String dumpDir = "logs/cointcore-stalls";

        @SerializedName("notify_discord")
        private boolean notifyDiscord = true;
    }
}
