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

public final class SparkProfilerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double MIN_MSPT = 1.0D;
    private static final double MAX_MSPT = 1000.0D;
    private static final int MIN_SAVE_INTERVAL_MINUTES = 1;
    private static final int MAX_SAVE_INTERVAL_MINUTES = 24 * 60;
    private static final int MIN_CLEAR_DELAY_SECONDS = 5;
    private static final int MAX_CLEAR_DELAY_SECONDS = 3600;
    private static final int MIN_ONLY_TICKS_OVER_MS = 0;
    private static final int MAX_ONLY_TICKS_OVER_MS = 60_000;

    private static boolean enabled = true;
    private static double msptThreshold = 50.0D;
    private static double msptClearThreshold = 45.0D;
    private static int saveIntervalMinutes = 30;
    private static int clearDelaySeconds = 60;
    private static int onlyTicksOverMs = 60;
    private static String commentPrefix = "cointcore-auto";

    private SparkProfilerConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getMsptThreshold() {
        return msptThreshold;
    }

    public static double getMsptClearThreshold() {
        return msptClearThreshold;
    }

    public static int getSaveIntervalMinutes() {
        return saveIntervalMinutes;
    }

    public static int getClearDelaySeconds() {
        return clearDelaySeconds;
    }

    public static String getCommentPrefix() {
        return commentPrefix;
    }

    public static int getOnlyTicksOverMs() {
        return onlyTicksOverMs;
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
                "Reloaded spark profiler config (enabled={}, msptThreshold={}, onlyTicksOverMs={}, saveIntervalMinutes={})",
                enabled,
                msptThreshold,
                onlyTicksOverMs,
                saveIntervalMinutes
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        msptThreshold = loaded.msptThreshold();
        msptClearThreshold = loaded.msptClearThreshold();
        saveIntervalMinutes = loaded.saveIntervalMinutes();
        clearDelaySeconds = loaded.clearDelaySeconds();
        onlyTicksOverMs = loaded.onlyTicksOverMs();
        commentPrefix = loaded.commentPrefix();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default spark profiler config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load spark profiler config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading spark profiler config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        double threshold = clampMspt(data.msptThreshold, 50.0D);
        double clearThreshold = clampMspt(data.msptClearThreshold, 45.0D);
        if (clearThreshold >= threshold) {
            clearThreshold = Math.max(MIN_MSPT, threshold - 1.0D);
        }

        String prefix = data.commentPrefix == null || data.commentPrefix.isBlank()
                ? "cointcore-auto"
                : data.commentPrefix.trim();

        return new LoadedConfig(
                data.enabled,
                threshold,
                clearThreshold,
                clampSaveIntervalMinutes(data.saveIntervalMinutes),
                clampClearDelaySeconds(data.clearDelaySeconds),
                clampOnlyTicksOverMs(data.onlyTicksOverMs),
                prefix
        );
    }

    private static double clampMspt(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(MIN_MSPT, Math.min(MAX_MSPT, value));
    }

    private static int clampSaveIntervalMinutes(int value) {
        return Math.max(MIN_SAVE_INTERVAL_MINUTES, Math.min(MAX_SAVE_INTERVAL_MINUTES, value));
    }

    private static int clampClearDelaySeconds(int value) {
        return Math.max(MIN_CLEAR_DELAY_SECONDS, Math.min(MAX_CLEAR_DELAY_SECONDS, value));
    }

    private static int clampOnlyTicksOverMs(int value) {
        return Math.max(MIN_ONLY_TICKS_OVER_MS, Math.min(MAX_ONLY_TICKS_OVER_MS, value));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("spark-profiler.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.msptThreshold = 50.0D;
        data.msptClearThreshold = 45.0D;
        data.saveIntervalMinutes = 30;
        data.clearDelaySeconds = 60;
        data.onlyTicksOverMs = 60;
        data.commentPrefix = "cointcore-auto";
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            double msptThreshold,
            double msptClearThreshold,
            int saveIntervalMinutes,
            int clearDelaySeconds,
            int onlyTicksOverMs,
            String commentPrefix
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private boolean enabled = true;

        @SerializedName("msptThreshold")
        private double msptThreshold = 50.0D;

        @SerializedName("msptClearThreshold")
        private double msptClearThreshold = 45.0D;

        @SerializedName("saveIntervalMinutes")
        private int saveIntervalMinutes = 30;

        @SerializedName("clearDelaySeconds")
        private int clearDelaySeconds = 60;

        @SerializedName("onlyTicksOverMs")
        private int onlyTicksOverMs = 60;

        @SerializedName("commentPrefix")
        private String commentPrefix = "cointcore-auto";
    }
}
