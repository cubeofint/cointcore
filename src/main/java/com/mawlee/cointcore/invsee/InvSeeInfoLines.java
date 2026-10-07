package com.mawlee.cointcore.invsee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats read-only InvSee info tabs from already-collected strings/numbers.
 */
public final class InvSeeInfoLines {
    private InvSeeInfoLines() {
    }

    public static List<String> playerState(
            String name,
            boolean online,
            float health,
            float maxHealth,
            int food,
            float saturation,
            int xpLevel,
            float xpProgress,
            int xpTotal,
            String dimension,
            double x,
            double y,
            double z,
            String gameMode,
            List<String> effects
    ) {
        List<String> lines = new ArrayList<>();
        lines.add(safe(name) + " — " + (online ? "online" : "offline"));
        lines.add("health " + formatFloat(health) + " / " + formatFloat(maxHealth));
        lines.add("food " + food + "  saturation " + formatFloat(saturation));
        lines.add("xp L" + xpLevel + "  bar " + percent(xpProgress) + "  total " + xpTotal);
        lines.add("pos " + formatFloat((float) x) + " " + formatFloat((float) y) + " " + formatFloat((float) z));
        lines.add("dimension " + safe(dimension));
        lines.add("gamemode " + safe(gameMode));
        if (effects == null || effects.isEmpty()) {
            lines.add("effects none");
        } else {
            lines.add("effects:");
            for (String effect : effects) {
                lines.add("  " + effect);
            }
        }
        return lines;
    }

    public static List<String> ftb(
            boolean available,
            String nick,
            List<String> homes,
            String lastDeath
    ) {
        List<String> lines = new ArrayList<>();
        if (!available) {
            lines.add("FTB Essentials is not loaded.");
            return lines;
        }
        lines.add("nick " + (blank(nick) ? "-" : nick));
        lines.add("last death " + (blank(lastDeath) ? "-" : lastDeath));
        if (homes == null || homes.isEmpty()) {
            lines.add("homes none");
        } else {
            lines.add("homes (" + homes.size() + "):");
            for (String home : homes) {
                lines.add("  " + home);
            }
        }
        return lines;
    }

    public static List<String> graves(
            boolean gravesModPresent,
            String vanillaLastDeath,
            List<String> graveLines
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("last death " + (blank(vanillaLastDeath) ? "-" : vanillaLastDeath));
        if (!gravesModPresent) {
            lines.add("graves mod is not loaded.");
            return lines;
        }
        if (graveLines == null || graveLines.isEmpty()) {
            lines.add("graves none");
        } else {
            lines.add("graves (" + graveLines.size() + "):");
            for (String grave : graveLines) {
                lines.add("  " + grave);
            }
        }
        return lines;
    }

    private static String percent(float value) {
        return Math.round(value * 100.0f) + "%";
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String safe(String value) {
        return blank(value) ? "-" : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
