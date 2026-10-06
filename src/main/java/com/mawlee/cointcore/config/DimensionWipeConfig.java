package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DimensionWipeConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static Settings settings = Settings.disabled();

    private DimensionWipeConfig() {
    }

    public static Settings get() {
        return settings;
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
        return true;
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default dimension wipe config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded dimension wipe config (enabled={}, schedule={}, dims={}, zone={})",
                        loaded.settings.enabled(),
                        loaded.settings.schedule().size(),
                        loaded.settings.dimensions().size(),
                        loaded.settings.zoneId()
                );
                if (reloading) {
                    LOGGER.info("Reloaded dimension wipe config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load dimension wipe config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading dimension wipe config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        settings = loaded.settings;
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(normalize(data));
    }

    private static Settings normalize(FileData data) {
        if (data == null) {
            return Settings.disabled();
        }

        List<ScheduleSlot> schedule = parseSchedule(data.schedule);
        ZoneId zoneId = parseZoneId(data.timezone);
        List<Integer> warnings = normalizeWarnings(data.warningsMinutesBefore);
        List<ResourceLocation> dimensions = parseDimensions(data.dimensions);
        int restartDelay = Math.max(30, Math.min(300, data.restartDelaySeconds > 0 ? data.restartDelaySeconds : 60));

        return new Settings(
                data.enabled,
                schedule,
                zoneId,
                restartDelay,
                warnings,
                dimensions,
                data.allowOverworldWipe
        );
    }

    private static List<ScheduleSlot> parseSchedule(List<ScheduleEntryFile> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<ScheduleSlot> slots = new ArrayList<>();
        for (ScheduleEntryFile entry : source) {
            if (entry == null || entry.date == null || entry.time == null) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(entry.date.trim(), DATE_FORMAT);
                LocalTime time = LocalTime.parse(entry.time.trim(), TIME_FORMAT).withSecond(0).withNano(0);
                String id = date.format(DATE_FORMAT) + ":" + time.format(TIME_FORMAT);
                if (!seen.add(id)) {
                    continue;
                }
                slots.add(new ScheduleSlot(date, time, id));
            } catch (DateTimeParseException exception) {
                LOGGER.warn("Invalid dimension wipe schedule entry date='{}' time='{}', skipping", entry.date, entry.time);
            }
        }

        slots.sort(Comparator
                .comparing(ScheduleSlot::date)
                .thenComparing(ScheduleSlot::time));
        return List.copyOf(slots);
    }

    private static List<ResourceLocation> parseDimensions(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<ResourceLocation> unique = new LinkedHashSet<>();
        for (String raw : source) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                unique.add(ResourceLocation.parse(raw.trim()));
            } catch (RuntimeException exception) {
                LOGGER.warn("Invalid dimension wipe id '{}', skipping", raw);
            }
        }
        return List.copyOf(unique);
    }

    private static ZoneId parseZoneId(String raw) {
        if (raw == null || raw.isBlank() || "SERVER".equalsIgnoreCase(raw.trim())) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (Exception exception) {
            LOGGER.warn("Invalid timezone '{}', using server default {}", raw, ZoneId.systemDefault());
            return ZoneId.systemDefault();
        }
    }

    private static List<Integer> normalizeWarnings(List<Integer> source) {
        if (source == null || source.isEmpty()) {
            return List.of(30, 10, 5, 1);
        }
        List<Integer> warnings = new ArrayList<>();
        for (Integer value : source) {
            if (value != null && value > 0) {
                warnings.add(value);
            }
        }
        return warnings.isEmpty() ? List.of(30, 10, 5, 1) : List.copyOf(warnings);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("dimension-wipe.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = false;
        data.timezone = "Europe/Moscow";
        data.restartDelaySeconds = 60;
        data.warningsMinutesBefore = List.of(30, 10, 5, 1);
        data.dimensions = List.of("minecraft:the_nether", "minecraft:the_end");
        data.allowOverworldWipe = false;

        ScheduleEntryFile example = new ScheduleEntryFile();
        example.date = "2099-01-01";
        example.time = "04:00";
        data.schedule = List.of(example);
        return data;
    }

    public record ScheduleSlot(LocalDate date, LocalTime time, String id) {
    }

    public record Settings(
            boolean enabled,
            List<ScheduleSlot> schedule,
            ZoneId zoneId,
            int restartDelaySeconds,
            List<Integer> warningsMinutesBefore,
            List<ResourceLocation> dimensions,
            boolean allowOverworldWipe
    ) {
        public static Settings disabled() {
            return new Settings(
                    false,
                    List.of(),
                    ZoneId.of("Europe/Moscow"),
                    60,
                    List.of(30, 10, 5, 1),
                    List.of(),
                    false
            );
        }
    }

    private record LoadedConfig(Settings settings) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private boolean enabled;

        @SerializedName("timezone")
        private String timezone;

        @SerializedName("schedule")
        private List<ScheduleEntryFile> schedule;

        @SerializedName("warningsMinutesBefore")
        private List<Integer> warningsMinutesBefore;

        @SerializedName("restartDelaySeconds")
        private int restartDelaySeconds;

        @SerializedName("dimensions")
        private List<String> dimensions;

        @SerializedName("allowOverworldWipe")
        private boolean allowOverworldWipe;
    }

    private static final class ScheduleEntryFile {
        @SerializedName("date")
        private String date;

        @SerializedName("time")
        private String time;
    }
}
