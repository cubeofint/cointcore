package com.mawlee.cointcore.mute;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String formatDuration(long ms) {
        if (ms == Long.MAX_VALUE) {
            return "forever";
        }

        long seconds = ms / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0L) {
            return days + "д " + (hours % 24L) + "ч";
        }

        if (hours > 0L) {
            return hours + "ч " + (minutes % 60L) + "м";
        }

        if (minutes > 0L) {
            return minutes + "м " + (seconds % 60L) + "с";
        }

        return seconds + "с";
    }
}
