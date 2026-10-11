package com.mawlee.cointcore.player;

import java.util.UUID;

/**
 * Parses player ids accepted by admin commands and AzLink (dashed or 32-char hex).
 */
public final class PlayerIds {
    private PlayerIds() {
    }

    public static UUID parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() == 32 && value.indexOf('-') < 0) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16)
                    + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
