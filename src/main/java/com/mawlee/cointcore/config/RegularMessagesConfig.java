package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.reminder.RegularSchedule;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Regular chat reminders managed with {@code /addreg}, {@code /reglist}, {@code /delreg} (port of the CointCoreGTO
 * reminders). Replaces the old {@code periodicMessages} rotation of {@code server-automation.json}.
 *
 * <p>File: {@code config/cointcore/regular-messages.json}. Commands rewrite it; hand edits need {@code /cointcore reload}.
 */
public final class RegularMessagesConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Settings settings = Settings.defaults();
    private static List<Entry> entries = List.of();

    private RegularMessagesConfig() {
    }

    public static Settings get() {
        return settings;
    }

    /** Valid entries sorted by id. */
    public static List<Entry> entries() {
        return entries;
    }

    public static Optional<Entry> find(int id) {
        return entries.stream().filter(entry -> entry.id() == id).findFirst();
    }

    public static void load() {
        Loaded loaded = loadFromDisk(false);
        apply(loaded);
    }

    public static boolean reload() {
        Loaded loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info("Reloaded regular messages ({} entries, enabled={})", entries.size(), settings.enabled());
        return true;
    }

    /** Adds a message with the next free number and saves; empty if the file could not be written. */
    public static Optional<Entry> add(RegularSchedule schedule, String text, String author) {
        int id = entries.stream().mapToInt(Entry::id).max().orElse(0) + 1;
        Entry entry = new Entry(id, schedule, text, author);
        List<Entry> updated = new ArrayList<>(entries);
        updated.add(entry);
        if (!save(settings, updated)) {
            return Optional.empty();
        }
        entries = List.copyOf(updated);
        return Optional.of(entry);
    }

    /** Removes a message and saves; false if it does not exist or the file could not be written. */
    public static boolean remove(int id) {
        List<Entry> updated = new ArrayList<>(entries);
        if (!updated.removeIf(entry -> entry.id() == id) || !save(settings, updated)) {
            return false;
        }
        entries = List.copyOf(updated);
        return true;
    }

    private static void apply(Loaded loaded) {
        settings = loaded.settings();
        entries = loaded.entries();
    }

    private static Loaded loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = new FileData();
                write(defaults, path);
                LOGGER.info("Created default regular messages config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : new FileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load regular messages from {}", path, exception);
            return reloading ? null : parse(new FileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading regular messages from {}", path, exception);
            return reloading ? null : parse(new FileData());
        }
    }

    private static Loaded parse(FileData data) {
        Settings parsed = new Settings(
                data.enabled,
                parseZone(data.timezone),
                Math.max(0, Math.min(24 * 60, data.minGapMinutes)),
                Math.max(1, data.minOnlinePlayers)
        );
        List<Entry> parsedEntries = new ArrayList<>();
        if (data.messages != null) {
            for (EntryData raw : data.messages) {
                if (raw == null || raw.id <= 0 || raw.text == null || raw.text.isBlank()) {
                    continue;
                }
                if (parsedEntries.stream().anyMatch(entry -> entry.id() == raw.id)) {
                    LOGGER.warn("Regular message #{} is listed twice; keeping the first", raw.id);
                    continue;
                }
                Optional<RegularSchedule> schedule = RegularSchedule.parse(raw.schedule);
                if (schedule.isEmpty()) {
                    LOGGER.warn("Regular message #{} has an invalid schedule '{}'; skipped", raw.id, raw.schedule);
                    continue;
                }
                parsedEntries.add(new Entry(raw.id, schedule.get(), raw.text, raw.author));
            }
        }
        parsedEntries.sort(Comparator.comparingInt(Entry::id));
        return new Loaded(parsed, List.copyOf(parsedEntries));
    }

    private static ZoneId parseZone(String raw) {
        if (raw == null || raw.isBlank() || "SERVER".equalsIgnoreCase(raw.trim())) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (DateTimeException exception) {
            LOGGER.warn("Unknown timezone '{}' in regular messages; using server time", raw);
            return ZoneId.systemDefault();
        }
    }

    private static boolean save(Settings current, List<Entry> list) {
        FileData data = new FileData();
        data.enabled = current.enabled();
        data.timezone = current.zone().getId();
        data.minGapMinutes = current.minGapMinutes();
        data.minOnlinePlayers = current.minOnlinePlayers();
        data.messages = new ArrayList<>();
        for (Entry entry : list) {
            EntryData raw = new EntryData();
            raw.id = entry.id();
            raw.schedule = entry.schedule().describe();
            raw.text = entry.text();
            raw.author = entry.author();
            data.messages.add(raw);
        }
        try {
            write(data, configPath());
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to save regular messages to {}", configPath(), exception);
            return false;
        }
    }

    private static void write(FileData data, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
        Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("regular-messages.json");
    }

    public record Settings(boolean enabled, ZoneId zone, int minGapMinutes, int minOnlinePlayers) {
        static Settings defaults() {
            return parse(new FileData()).settings();
        }
    }

    public record Entry(int id, RegularSchedule schedule, String text, String author) {
    }

    private record Loaded(Settings settings, List<Entry> entries) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Zone of the "N/д" slots; "SERVER" = the machine's zone. */
        @SerializedName("timezone")
        String timezone = "Europe/Moscow";

        /** Minimum time between two reminders; messages due at once are sent this far apart in id order. */
        @SerializedName("min_gap_minutes")
        int minGapMinutes = 15;

        /** Reminders wait while fewer players are online. */
        @SerializedName("min_online_players")
        int minOnlinePlayers = 1;

        @SerializedName("messages")
        List<EntryData> messages = new ArrayList<>();
    }

    private static final class EntryData {
        @SerializedName("id")
        int id;

        /** "4/д" — 4 times a day on wall-clock slots; "+1ч30м" — once after server start. */
        @SerializedName("schedule")
        String schedule;

        @SerializedName("text")
        String text;

        @SerializedName("author")
        String author;
    }
}
