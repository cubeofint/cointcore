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
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ServerAutomationConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter RESTART_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static ScheduledRestartSettings scheduledRestart = ScheduledRestartSettings.disabled();
    private static PeriodicMessageSettings periodicMessages = PeriodicMessageSettings.disabled();
    private static RestartCountdownSettings restartCountdown = RestartCountdownSettings.defaults();

    private ServerAutomationConfig() {
    }

    public static ScheduledRestartSettings getScheduledRestart() {
        return scheduledRestart;
    }

    public static PeriodicMessageSettings getPeriodicMessages() {
        return periodicMessages;
    }

    public static RestartCountdownSettings getRestartCountdown() {
        return restartCountdown;
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
                LOGGER.info("Created default server automation config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                LoadedConfig loaded = parse(data != null ? data : defaultFileData());
                LOGGER.info(
                        "Loaded server automation config (restart: {}, times: {} {}, periodic messages: {})",
                        loaded.scheduledRestart.enabled(),
                        loaded.scheduledRestart.times().stream()
                                .map(time -> time.format(RESTART_TIME_FORMAT))
                                .toList(),
                        loaded.scheduledRestart.zoneId(),
                        loaded.periodicMessages.enabled()
                );
                if (reloading) {
                    LOGGER.info("Reloaded server automation config from {}", path);
                }
                return loaded;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load server automation config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading server automation config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static void apply(LoadedConfig loaded) {
        scheduledRestart = loaded.scheduledRestart;
        periodicMessages = loaded.periodicMessages;
        restartCountdown = loaded.restartCountdown;
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(
                normalizeScheduledRestart(data.scheduledRestart),
                normalizePeriodicMessages(data.periodicMessages),
                normalizeRestartCountdown(data.restartCountdown)
        );
    }

    private static ScheduledRestartSettings normalizeScheduledRestart(ScheduledRestartFile source) {
        if (source == null) {
            return ScheduledRestartSettings.disabled();
        }

        LocalTime fallbackTime = LocalTime.of(4, 0);
        List<LocalTime> times = parseTimes(source, fallbackTime);
        ZoneId zoneId = parseZoneId(source.timezone);
        List<Integer> warnings = normalizeWarnings(source.warningsMinutesBefore);

        return new ScheduledRestartSettings(
                source.enabled,
                times,
                zoneId,
                Math.max(30, Math.min(300, source.restartDelaySeconds)),
                warnings
        );
    }

    private static List<LocalTime> parseTimes(ScheduledRestartFile source, LocalTime fallbackTime) {
        Set<LocalTime> unique = new LinkedHashSet<>();
        if (source.times != null) {
            for (String raw : source.times) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    unique.add(LocalTime.parse(raw.trim()).withSecond(0).withNano(0));
                } catch (DateTimeParseException exception) {
                    LOGGER.warn("Invalid restart time '{}', skipping", raw);
                }
            }
        }
        if (unique.isEmpty() && source.time != null && !source.time.isBlank()) {
            unique.add(parseTime(source.time, fallbackTime));
        }
        if (unique.isEmpty()) {
            unique.add(fallbackTime);
        }

        List<LocalTime> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.naturalOrder());
        return List.copyOf(sorted);
    }

    private static PeriodicMessageSettings normalizePeriodicMessages(PeriodicMessagesFile source) {
        if (source == null) {
            return PeriodicMessageSettings.disabled();
        }

        List<String> messages = new ArrayList<>();
        if (source.messages != null) {
            for (String message : source.messages) {
                if (message != null && !message.isBlank()) {
                    messages.add(message.trim());
                }
            }
        }

        return new PeriodicMessageSettings(
                source.enabled,
                Math.max(30, source.intervalSeconds),
                List.copyOf(messages)
        );
    }

    private static RestartCountdownSettings normalizeRestartCountdown(RestartCountdownFile source) {
        if (source == null) {
            return RestartCountdownSettings.defaults();
        }

        List<Integer> warnings = normalizeCountdownWarnings(source.warningSecondsBefore);
        String warningText = source.warningText == null || source.warningText.isBlank()
                ? RestartCountdownSettings.defaults().warningText()
                : source.warningText.trim();
        String titleText = source.titleText == null || source.titleText.isBlank()
                ? RestartCountdownSettings.defaults().titleText()
                : source.titleText.trim();

        int kickPlayersSecondsBefore = source.kickPlayersSecondsBefore > 0
                ? source.kickPlayersSecondsBefore
                : 5;

        return new RestartCountdownSettings(
                warnings,
                warningText,
                source.showTitleOnFirstWarning,
                titleText,
                Math.max(1, Math.min(300, kickPlayersSecondsBefore))
        );
    }

    private static List<Integer> normalizeCountdownWarnings(List<Integer> source) {
        if (source == null || source.isEmpty()) {
            return RestartCountdownSettings.defaults().warningSecondsBefore();
        }

        List<Integer> warnings = new ArrayList<>();
        for (Integer value : source) {
            if (value != null && value > 0) {
                warnings.add(value);
            }
        }
        if (warnings.isEmpty()) {
            return RestartCountdownSettings.defaults().warningSecondsBefore();
        }
        warnings.sort(Comparator.naturalOrder());
        return List.copyOf(warnings);
    }

    private static LocalTime parseTime(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException exception) {
            LOGGER.warn("Invalid restart time '{}', using {}", raw, fallback);
            return fallback;
        }
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
            return List.of(15, 5, 1);
        }
        List<Integer> warnings = new ArrayList<>();
        for (Integer value : source) {
            if (value != null && value > 0) {
                warnings.add(value);
            }
        }
        return warnings.isEmpty() ? List.of(15, 5, 1) : List.copyOf(warnings);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("server-automation.json");
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();

        ScheduledRestartFile restart = new ScheduledRestartFile();
        restart.enabled = false;
        restart.times = List.of("04:00", "12:00", "20:00");
        restart.timezone = "SERVER";
        restart.restartDelaySeconds = 60;
        restart.warningsMinutesBefore = List.of(15, 5, 1);
        data.scheduledRestart = restart;

        RestartCountdownFile countdown = new RestartCountdownFile();
        countdown.warningSecondsBefore = List.of(60, 30, 15, 5, 1);
        countdown.warningText = "&cПЕРЕЗАГРУЗКА ЧЕРЕЗ % СЕК";
        countdown.showTitleOnFirstWarning = true;
        countdown.titleText = "ПЕРЕЗАГРУЗКА СЕРВЕРА";
        countdown.kickPlayersSecondsBefore = 5;
        data.restartCountdown = countdown;

        PeriodicMessagesFile messages = new PeriodicMessagesFile();
        messages.enabled = false;
        messages.intervalSeconds = 600;
        messages.messages = List.of(
                "&6Добро пожаловать на сервер!",
                "&7Discord: &bdiscord.gg/example"
        );
        data.periodicMessages = messages;

        return data;
    }

    public record ScheduledRestartSettings(
            boolean enabled,
            List<LocalTime> times,
            ZoneId zoneId,
            int restartDelaySeconds,
            List<Integer> warningsMinutesBefore
    ) {
        public static ScheduledRestartSettings disabled() {
            return new ScheduledRestartSettings(
                    false,
                    List.of(LocalTime.of(4, 0)),
                    ZoneId.systemDefault(),
                    60,
                    List.of(15, 5, 1)
            );
        }
    }

    public record RestartCountdownSettings(
            List<Integer> warningSecondsBefore,
            String warningText,
            boolean showTitleOnFirstWarning,
            String titleText,
            int kickPlayersSecondsBefore
    ) {
        public static RestartCountdownSettings defaults() {
            return new RestartCountdownSettings(
                    List.of(60, 30, 15, 5, 1),
                    "&cПЕРЕЗАГРУЗКА ЧЕРЕЗ % СЕК",
                    true,
                    "ПЕРЕЗАГРУЗКА СЕРВЕРА",
                    5
            );
        }
    }

    public record PeriodicMessageSettings(
            boolean enabled,
            int intervalSeconds,
            List<String> messages
    ) {
        public static PeriodicMessageSettings disabled() {
            return new PeriodicMessageSettings(false, 600, List.of());
        }
    }

    private record LoadedConfig(
            ScheduledRestartSettings scheduledRestart,
            PeriodicMessageSettings periodicMessages,
            RestartCountdownSettings restartCountdown
    ) {
    }

    private static final class FileData {
        @SerializedName("scheduledRestart")
        private ScheduledRestartFile scheduledRestart;

        @SerializedName("periodicMessages")
        private PeriodicMessagesFile periodicMessages;

        @SerializedName("restartCountdown")
        private RestartCountdownFile restartCountdown;
    }

    private static final class ScheduledRestartFile {
        @SerializedName("enabled")
        private boolean enabled;

        @SerializedName("times")
        private List<String> times;

        /** @deprecated Use {@link #times}. Still read for backward compatibility. */
        @SerializedName("time")
        private String time;

        @SerializedName("timezone")
        private String timezone;

        @SerializedName("restartDelaySeconds")
        private int restartDelaySeconds;

        @SerializedName("warningsMinutesBefore")
        private List<Integer> warningsMinutesBefore;
    }

    private static final class PeriodicMessagesFile {
        @SerializedName("enabled")
        private boolean enabled;

        @SerializedName("intervalSeconds")
        private int intervalSeconds;

        @SerializedName("messages")
        private List<String> messages;
    }

    private static final class RestartCountdownFile {
        @SerializedName("warningSecondsBefore")
        private List<Integer> warningSecondsBefore;

        @SerializedName("warningText")
        private String warningText;

        @SerializedName("showTitleOnFirstWarning")
        private boolean showTitleOnFirstWarning = true;

        @SerializedName("titleText")
        private String titleText;

        @SerializedName("kickPlayersSecondsBefore")
        private int kickPlayersSecondsBefore = 5;

        @SerializedName("saveWorldSecondsBefore")
        private int saveWorldSecondsBefore;
    }
}
