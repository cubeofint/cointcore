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
 * Server config for the tick watchdog.
 *
 * <p>Defaults are production-safe: always-on tick stats (~2× nanoTime/tick),
 * detailed BE/entity timing only after recent slow ticks, stack sampling only
 * during armed slow-tick windows. Estimated idle overhead: &lt;0.01 ms/tick;
 * detailed mode typically 0.05–0.3 ms/tick depending on BE/entity count;
 * sampling ~0.01–0.05 ms per sample on the sampler thread (not server thread).
 */
public final class TickWatchdogConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static double slowTickThresholdMs = 100.0D;
    private static int sustainedSlowTicks = 3;
    private static int windowSeconds = 60;
    private static String detailedTimingMode = "auto";
    private static int autoDetailedAfterSlowTicks = 1;
    private static int samplingIntervalMs = 1;
    private static int samplingMaxDurationMs = 80;
    private static int topEntries = 15;
    private static int reportIntervalSeconds = 300;
    private static int maxReportAgeDays = 7;
    private static int maxReportTotalSizeMb = 512;
    private static boolean notifyAdmins = true;
    private static int notifyCooldownSeconds = 60;
    private static boolean attributeEntities = true;
    private static boolean attributeBlockEntities = true;

    private TickWatchdogConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getSlowTickThresholdMs() {
        return slowTickThresholdMs;
    }

    public static long getSlowTickThresholdNanos() {
        return (long) (slowTickThresholdMs * 1_000_000.0D);
    }

    public static int getSustainedSlowTicks() {
        return sustainedSlowTicks;
    }

    public static int getWindowSeconds() {
        return windowSeconds;
    }

    public static String getDetailedTimingMode() {
        return detailedTimingMode;
    }

    public static boolean isDetailedAlways() {
        return "always".equalsIgnoreCase(detailedTimingMode);
    }

    public static boolean isDetailedOff() {
        return "off".equalsIgnoreCase(detailedTimingMode);
    }

    public static int getAutoDetailedAfterSlowTicks() {
        return autoDetailedAfterSlowTicks;
    }

    public static int getSamplingIntervalMs() {
        return samplingIntervalMs;
    }

    public static int getSamplingMaxDurationMs() {
        return samplingMaxDurationMs;
    }

    public static int getTopEntries() {
        return topEntries;
    }

    public static int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    public static int getMaxReportAgeDays() {
        return maxReportAgeDays;
    }

    public static int getMaxReportTotalSizeMb() {
        return maxReportTotalSizeMb;
    }

    public static long getMaxReportTotalSizeBytes() {
        return maxReportTotalSizeMb * 1024L * 1024L;
    }

    public static boolean isNotifyAdmins() {
        return notifyAdmins;
    }

    public static int getNotifyCooldownSeconds() {
        return notifyCooldownSeconds;
    }

    public static boolean isAttributeEntities() {
        return attributeEntities;
    }

    public static boolean isAttributeBlockEntities() {
        return attributeBlockEntities;
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
                "Reloaded tick watchdog config (enabled={}, threshold={} ms, mode={}, window={}s)",
                enabled,
                slowTickThresholdMs,
                detailedTimingMode,
                windowSeconds
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        slowTickThresholdMs = loaded.slowTickThresholdMs();
        sustainedSlowTicks = loaded.sustainedSlowTicks();
        windowSeconds = loaded.windowSeconds();
        detailedTimingMode = loaded.detailedTimingMode();
        autoDetailedAfterSlowTicks = loaded.autoDetailedAfterSlowTicks();
        samplingIntervalMs = loaded.samplingIntervalMs();
        samplingMaxDurationMs = loaded.samplingMaxDurationMs();
        topEntries = loaded.topEntries();
        reportIntervalSeconds = loaded.reportIntervalSeconds();
        maxReportAgeDays = loaded.maxReportAgeDays();
        maxReportTotalSizeMb = loaded.maxReportTotalSizeMb();
        notifyAdmins = loaded.notifyAdmins();
        notifyCooldownSeconds = loaded.notifyCooldownSeconds();
        attributeEntities = loaded.attributeEntities();
        attributeBlockEntities = loaded.attributeBlockEntities();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default tick watchdog config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load tick watchdog config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading tick watchdog config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        String mode = data.detailedTimingMode == null || data.detailedTimingMode.isBlank()
                ? "auto"
                : data.detailedTimingMode.trim().toLowerCase();
        if (!mode.equals("auto") && !mode.equals("always") && !mode.equals("off")) {
            mode = "auto";
        }
        return new LoadedConfig(
                data.enabled,
                clamp(data.slowTickThresholdMs, 1.0D, 10_000.0D, 100.0D),
                clampInt(data.sustainedSlowTicks, 1, 200, 3),
                clampInt(data.windowSeconds, 10, 3600, 60),
                mode,
                clampInt(data.autoDetailedAfterSlowTicks, 1, 50, 1),
                clampInt(data.samplingIntervalMs, 1, 50, 1),
                clampInt(data.samplingMaxDurationMs, 5, 1000, 80),
                clampInt(data.topEntries, 3, 100, 15),
                clampInt(data.reportIntervalSeconds, 30, 86_400, 300),
                clampInt(data.maxReportAgeDays, 0, 365, 7),
                clampInt(data.maxReportTotalSizeMb, 0, 102_400, 512),
                data.notifyAdmins,
                clampInt(data.notifyCooldownSeconds, 5, 3600, 60),
                data.attributeEntities,
                data.attributeBlockEntities
        );
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("tick-watchdog.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.slowTickThresholdMs = 100.0D;
        data.sustainedSlowTicks = 3;
        data.windowSeconds = 60;
        data.detailedTimingMode = "auto";
        data.autoDetailedAfterSlowTicks = 1;
        data.samplingIntervalMs = 1;
        data.samplingMaxDurationMs = 80;
        data.topEntries = 15;
        data.reportIntervalSeconds = 300;
        data.maxReportAgeDays = 7;
        data.maxReportTotalSizeMb = 512;
        data.notifyAdmins = true;
        data.notifyCooldownSeconds = 60;
        data.attributeEntities = true;
        data.attributeBlockEntities = true;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            double slowTickThresholdMs,
            int sustainedSlowTicks,
            int windowSeconds,
            String detailedTimingMode,
            int autoDetailedAfterSlowTicks,
            int samplingIntervalMs,
            int samplingMaxDurationMs,
            int topEntries,
            int reportIntervalSeconds,
            int maxReportAgeDays,
            int maxReportTotalSizeMb,
            boolean notifyAdmins,
            int notifyCooldownSeconds,
            boolean attributeEntities,
            boolean attributeBlockEntities
    ) {
    }

    @SuppressWarnings("unused")
    private static final class FileData {
        @SerializedName("enabled")
        private boolean enabled = true;

        @SerializedName("slowTickThresholdMs")
        private double slowTickThresholdMs = 100.0D;

        @SerializedName("sustainedSlowTicks")
        private int sustainedSlowTicks = 3;

        @SerializedName("windowSeconds")
        private int windowSeconds = 60;

        @SerializedName("detailedTimingMode")
        private String detailedTimingMode = "auto";

        @SerializedName("autoDetailedAfterSlowTicks")
        private int autoDetailedAfterSlowTicks = 1;

        @SerializedName("samplingIntervalMs")
        private int samplingIntervalMs = 1;

        @SerializedName("samplingMaxDurationMs")
        private int samplingMaxDurationMs = 80;

        @SerializedName("topEntries")
        private int topEntries = 15;

        @SerializedName("reportIntervalSeconds")
        private int reportIntervalSeconds = 300;

        @SerializedName("maxReportAgeDays")
        private int maxReportAgeDays = 7;

        @SerializedName("maxReportTotalSizeMb")
        private int maxReportTotalSizeMb = 512;

        @SerializedName("notifyAdmins")
        private boolean notifyAdmins = true;

        @SerializedName("notifyCooldownSeconds")
        private int notifyCooldownSeconds = 60;

        @SerializedName("attributeEntities")
        private boolean attributeEntities = true;

        @SerializedName("attributeBlockEntities")
        private boolean attributeBlockEntities = true;
    }
}
