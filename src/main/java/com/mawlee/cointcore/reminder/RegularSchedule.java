package com.mawlee.cointcore.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When a regular message fires. Minecraft-free, unit-tested.
 *
 * <ul>
 *     <li>{@code 4/д} ({@code 4/d}) — N times a day on wall-clock slots from midnight: 00:00, 06:00, 12:00, 18:00.
 *     Restarts do not shift them.</li>
 *     <li>{@code +1ч30м} ({@code +1h30m}) — once, that long after server start.</li>
 * </ul>
 */
public sealed interface RegularSchedule {
    int MAX_TIMES_PER_DAY = 96;
    long MAX_AFTER_START_MINUTES = 7 * 24 * 60;
    long NONE = -1L;

    Pattern DAILY = Pattern.compile("(\\d{1,3})/(д|d|сут|day)");
    Pattern AFTER_START = Pattern.compile("\\+(?:(\\d{1,4})(ч|h))?(?:(\\d{1,5})(м|m))?");

    /** Canonical form stored in the config and shown in {@code /reglist}. */
    String describe();

    /** Latest slot at or before {@code nowMs}, or {@link #NONE}. */
    long latestSlot(long nowMs, long startMs, ZoneId zone);

    /** First slot after {@code nowMs}, or {@link #NONE}. */
    long nextSlot(long nowMs, long startMs, ZoneId zone);

    static Optional<RegularSchedule> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String token = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher daily = DAILY.matcher(token);
        if (daily.matches()) {
            int times = Integer.parseInt(daily.group(1));
            return times >= 1 && times <= MAX_TIMES_PER_DAY ? Optional.of(new Daily(times)) : Optional.empty();
        }
        Matcher after = AFTER_START.matcher(token);
        if (after.matches() && (after.group(1) != null || after.group(3) != null)) {
            long hours = after.group(1) != null ? Long.parseLong(after.group(1)) : 0L;
            long minutes = after.group(3) != null ? Long.parseLong(after.group(3)) : 0L;
            long total = hours * 60L + minutes;
            return total >= 1 && total <= MAX_AFTER_START_MINUTES ? Optional.of(new AfterStart(total)) : Optional.empty();
        }
        return Optional.empty();
    }

    record Daily(int timesPerDay) implements RegularSchedule {
        @Override
        public String describe() {
            return timesPerDay + "/д";
        }

        @Override
        public long latestSlot(long nowMs, long startMs, ZoneId zone) {
            LocalDate day = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate();
            long midnight = midnight(day, zone);
            int slot = -1;
            while (slot + 1 < timesPerDay && slotMs(midnight, slot + 1) <= nowMs) {
                slot++;
            }
            return slot >= 0 ? slotMs(midnight, slot) : slotMs(midnight(day.minusDays(1), zone), timesPerDay - 1);
        }

        @Override
        public long nextSlot(long nowMs, long startMs, ZoneId zone) {
            LocalDate day = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate();
            long midnight = midnight(day, zone);
            for (int slot = 0; slot < timesPerDay; slot++) {
                long at = slotMs(midnight, slot);
                if (at > nowMs) {
                    return at;
                }
            }
            return midnight(day.plusDays(1), zone);
        }

        private long slotMs(long midnight, int slot) {
            return midnight + slot * 1440L / timesPerDay * 60_000L;
        }

        private static long midnight(LocalDate day, ZoneId zone) {
            return day.atStartOfDay(zone).toInstant().toEpochMilli();
        }
    }

    record AfterStart(long minutes) implements RegularSchedule {
        @Override
        public String describe() {
            long hours = minutes / 60L;
            long rest = minutes % 60L;
            return "+" + (hours > 0 ? hours + "ч" : "") + (rest > 0 || hours == 0 ? rest + "м" : "");
        }

        @Override
        public long latestSlot(long nowMs, long startMs, ZoneId zone) {
            long at = startMs + minutes * 60_000L;
            return at <= nowMs ? at : NONE;
        }

        @Override
        public long nextSlot(long nowMs, long startMs, ZoneId zone) {
            long at = startMs + minutes * 60_000L;
            return at > nowMs ? at : NONE;
        }
    }
}
