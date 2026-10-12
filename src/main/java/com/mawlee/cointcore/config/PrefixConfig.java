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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * File: {@code config/cointcore/prefixes.json}.
 * Prefix picker and site sync settings.
 */
public final class PrefixConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "prefixes.json";
    public static final String DEFAULT_STAFF_TRACK = "staff";
    public static final String DEFAULT_RANKS_TRACK = "ranks";
    public static final String DEFAULT_META_KEY = "cointcore-prefix-select";
    public static final String DEFAULT_RENDER_META_KEY = "cointcore-quest-render";
    public static final int DEFAULT_PREFIX_PRIORITY = 5;
    public static final int DEFAULT_CATALOG_STARTUP_DELAY_SECONDS = 45;
    public static final int DEFAULT_CATALOG_DEBOUNCE_SECONDS = 5;
    public static final int DEFAULT_CATALOG_HASH_INTERVAL_SECONDS = 900;
    public static final int DEFAULT_REPORT_ALL_BATCH_SIZE = 40;
    public static final int DEFAULT_REPORT_ALL_BATCH_PAUSE_MS = 1500;
    public static final int DEFAULT_REPORT_ALL_CHUNK_SIZE = 150;
    public static final double DEFAULT_REPORT_ALL_MIN_TPS = 18.0D;

    private static Settings settings = Settings.defaults();

    private PrefixConfig() {
    }

    public static Settings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve(FILE_NAME);
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
        LOGGER.info("Reloaded prefix config from {}", getConfigPath());
        return true;
    }

    private static void apply(Settings loaded) {
        settings = loaded != null ? loaded : Settings.defaults();
    }

    private static Settings loadFromDisk(boolean reloading) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    ConfigMergeSupport.GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default prefix config at {}", path);
                return Settings.from(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = ConfigMergeSupport.GSON.fromJson(reader, FileData.class);
                Settings parsed = Settings.from(data);
                if (!reloading) {
                    LOGGER.info("Loaded prefix config from {}", path);
                }
                return parsed;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load prefix config from {}", path, exception);
            return reloading ? null : Settings.defaults();
        }
    }

    public record Settings(
            boolean enabled,
            String staffTrack,
            List<String> staffGroups,
            String ranksTrack,
            List<String> excludeGroups,
            List<String> donationGroups,
            List<String> mediaGroups,
            List<String> builderGroups,
            String defaultGroup,
            Map<String, String> categoryOverrides,
            String unmatchedCategory,
            int prefixPriority,
            String prefixMetaKey,
            String renderMetaKey,
            boolean siteSyncEnabled,
            int siteReportDebounceTicks,
            int sitePollSeconds,
            int catalogStartupDelaySeconds,
            int catalogDebounceSeconds,
            int catalogHashIntervalSeconds,
            int reportAllBatchSize,
            int reportAllBatchPauseMs,
            int reportAllChunkSize,
            double reportAllMinTps
    ) {
        static Settings defaults() {
            return from(FileData.defaults());
        }

        static Settings from(FileData data) {
            if (data == null) {
                data = FileData.defaults();
            }
            return new Settings(
                    data.enabled == null || data.enabled,
                    normalizeName(data.staffTrack, DEFAULT_STAFF_TRACK),
                    normalizeList(data.staffGroups, FileData.defaultStaffGroups()),
                    normalizeName(data.ranksTrack, DEFAULT_RANKS_TRACK),
                    normalizeList(data.excludeGroups, List.of()),
                    normalizeList(data.donationGroups, List.of()),
                    normalizeList(data.mediaGroups, FileData.defaultMediaGroups()),
                    normalizeList(data.builderGroups, FileData.defaultBuilderGroups()),
                    normalizeName(data.defaultGroup, "default"),
                    normalizeOverrides(data.categoryOverrides),
                    normalizeName(data.unmatchedCategory, "donation"),
                    data.prefixPriority == null || data.prefixPriority <= 0
                            ? DEFAULT_PREFIX_PRIORITY
                            : data.prefixPriority,
                    normalizeName(data.prefixMetaKey, DEFAULT_META_KEY),
                    normalizeName(data.renderMetaKey, DEFAULT_RENDER_META_KEY),
                    data.siteSyncEnabled == null || data.siteSyncEnabled,
                    clamp(data.siteReportDebounceTicks, 1, 400, 40),
                    clamp(data.sitePollSeconds, 1, 300, 15),
                    clamp(data.catalogStartupDelaySeconds, 1, 600, DEFAULT_CATALOG_STARTUP_DELAY_SECONDS),
                    clamp(data.catalogDebounceSeconds, 1, 60, DEFAULT_CATALOG_DEBOUNCE_SECONDS),
                    clamp(data.catalogHashIntervalSeconds, 30, 86_400, DEFAULT_CATALOG_HASH_INTERVAL_SECONDS),
                    clamp(data.reportAllBatchSize, 25, 50, DEFAULT_REPORT_ALL_BATCH_SIZE),
                    clamp(data.reportAllBatchPauseMs, 1000, 2000, DEFAULT_REPORT_ALL_BATCH_PAUSE_MS),
                    clamp(data.reportAllChunkSize, 100, 200, DEFAULT_REPORT_ALL_CHUNK_SIZE),
                    clampDouble(data.reportAllMinTps, 1.0D, 20.0D, DEFAULT_REPORT_ALL_MIN_TPS)
            );
        }
    }

    private static List<String> normalizeList(List<String> values, List<String> fallback) {
        List<String> source = values == null ? fallback : values;
        List<String> result = new ArrayList<>();
        for (String value : source) {
            String normalized = normalizeName(value, null);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, String> normalizeOverrides(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String group = normalizeName(entry.getKey(), null);
            String category = normalizeName(entry.getValue(), null);
            if (group != null && category != null) {
                result.put(group, category);
            }
        }
        return Map.copyOf(result);
    }

    private static String normalizeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp(Integer value, int min, int max, int fallback) {
        int parsed = value == null || value <= 0 ? fallback : value;
        return Math.max(min, Math.min(max, parsed));
    }

    private static double clampDouble(Double value, double min, double max, double fallback) {
        double parsed = value == null || value <= 0.0D ? fallback : value;
        return Math.max(min, Math.min(max, parsed));
    }

    static final class FileData {
        Boolean enabled;
        @SerializedName("staff_track")
        String staffTrack;
        @SerializedName("staff_groups")
        List<String> staffGroups;
        @SerializedName("ranks_track")
        String ranksTrack;
        @SerializedName("exclude_groups")
        List<String> excludeGroups;
        @SerializedName("donation_groups")
        List<String> donationGroups;
        @SerializedName("media_groups")
        List<String> mediaGroups;
        @SerializedName("builder_groups")
        List<String> builderGroups;
        @SerializedName("default_group")
        String defaultGroup;
        @SerializedName("category_overrides")
        Map<String, String> categoryOverrides;
        @SerializedName("unmatched_category")
        String unmatchedCategory;
        @SerializedName("prefix_priority")
        Integer prefixPriority;
        @SerializedName("prefix_meta_key")
        String prefixMetaKey;
        @SerializedName("render_meta_key")
        String renderMetaKey;
        @SerializedName("site_sync_enabled")
        Boolean siteSyncEnabled;
        @SerializedName("catalog_startup_delay_seconds")
        Integer catalogStartupDelaySeconds;
        @SerializedName("catalog_debounce_seconds")
        Integer catalogDebounceSeconds;
        @SerializedName("catalog_hash_interval_seconds")
        Integer catalogHashIntervalSeconds;
        @SerializedName("report_all_batch_size")
        Integer reportAllBatchSize;
        @SerializedName("report_all_batch_pause_ms")
        Integer reportAllBatchPauseMs;
        @SerializedName("report_all_chunk_size")
        Integer reportAllChunkSize;
        @SerializedName("report_all_min_tps")
        Double reportAllMinTps;
        @SerializedName("site_report_debounce_ticks")
        Integer siteReportDebounceTicks;
        @SerializedName("site_poll_seconds")
        Integer sitePollSeconds;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.staffTrack = DEFAULT_STAFF_TRACK;
            data.staffGroups = defaultStaffGroups();
            data.ranksTrack = DEFAULT_RANKS_TRACK;
            data.excludeGroups = List.of();
            data.donationGroups = List.of();
            data.mediaGroups = defaultMediaGroups();
            data.builderGroups = defaultBuilderGroups();
            data.defaultGroup = "default";
            data.categoryOverrides = Map.of();
            data.unmatchedCategory = "donation";
            data.prefixPriority = DEFAULT_PREFIX_PRIORITY;
            data.prefixMetaKey = DEFAULT_META_KEY;
            data.renderMetaKey = DEFAULT_RENDER_META_KEY;
            data.siteSyncEnabled = true;
            data.siteReportDebounceTicks = 40;
            data.sitePollSeconds = 15;
            data.catalogStartupDelaySeconds = DEFAULT_CATALOG_STARTUP_DELAY_SECONDS;
            data.catalogDebounceSeconds = DEFAULT_CATALOG_DEBOUNCE_SECONDS;
            data.catalogHashIntervalSeconds = DEFAULT_CATALOG_HASH_INTERVAL_SECONDS;
            data.reportAllBatchSize = DEFAULT_REPORT_ALL_BATCH_SIZE;
            data.reportAllBatchPauseMs = DEFAULT_REPORT_ALL_BATCH_PAUSE_MS;
            data.reportAllChunkSize = DEFAULT_REPORT_ALL_CHUNK_SIZE;
            data.reportAllMinTps = DEFAULT_REPORT_ALL_MIN_TPS;
            return data;
        }

        static List<String> defaultStaffGroups() {
            return List.of("stazher", "helper", "moderator", "hmod", "admin", "hadmin");
        }

        static List<String> defaultMediaGroups() {
            return List.of("media", "youtube", "twitch", "tiktok");
        }

        static List<String> defaultBuilderGroups() {
            return List.of("builder");
        }
    }
}
