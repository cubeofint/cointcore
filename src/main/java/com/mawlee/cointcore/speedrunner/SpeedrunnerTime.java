package com.mawlee.cointcore.speedrunner;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class SpeedrunnerTime {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private SpeedrunnerTime() {
    }

    public static String formatPlayTime(int ticks) {
        long seconds = Math.max(0, ticks) / 20L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + secs + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    public static String formatDate(long epochMs) {
        if (epochMs <= 0L) {
            return "-";
        }
        return DATE.format(Instant.ofEpochMilli(epochMs));
    }
}
