package com.mawlee.cointcore.privilege;

import java.util.Locale;

public enum ExpAddUnit {
    POINTS,
    LEVELS;

    public static ExpAddUnit parse(String raw, ExpAddUnit fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("points") || normalized.equals("point") || normalized.equals("xp")) {
            return POINTS;
        }
        if (normalized.equals("levels") || normalized.equals("level") || normalized.equals("lvl")) {
            return LEVELS;
        }
        return fallback;
    }

    public String configKey() {
        if (this == LEVELS) {
            return "levels";
        }
        return "points";
    }
}
