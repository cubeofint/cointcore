package com.mawlee.cointcore.watchdog;

import java.util.Locale;

/**
 * Resolves interned type / dimension keys to report strings. Hot path stores the keys only.
 */
public interface WatchdogLabels {
    WatchdogLabels STRINGS = new WatchdogLabels() {
        @Override
        public String typeId(Object type) {
            return stringify(type);
        }

        @Override
        public String dimensionId(Object dimension) {
            return stringify(dimension);
        }
    };

    String typeId(Object type);

    String dimensionId(Object dimension);

    static String stringify(Object value) {
        if (value == null) {
            return "unknown";
        }
        if (value instanceof String string) {
            if (string.isBlank()) {
                return "unknown";
            }
            return string.trim();
        }
        return "unknown";
    }

    static String owningMod(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return "unknown";
        }
        int colon = typeId.indexOf(':');
        if (colon > 0) {
            return typeId.substring(0, colon).toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }
}
