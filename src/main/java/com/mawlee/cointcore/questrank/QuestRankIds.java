package com.mawlee.cointcore.questrank;

import java.util.Locale;
import java.util.OptionalLong;

/**
 * FTB Quests object ids are unsigned 64-bit values shown as hex.
 */
public final class QuestRankIds {
    private QuestRankIds() {
    }

    public static OptionalLong parseHex(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        String value = raw.trim();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseUnsignedLong(value, 16));
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    public static String toHex(long questId) {
        return Long.toUnsignedString(questId, 16).toUpperCase(Locale.ROOT);
    }
}
