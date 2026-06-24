package com.mawlee.cointcore.punishment;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.TimeUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PunishmentFormatter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private PunishmentFormatter() {
    }

    public static String formatType(String language, PunishmentType type) {
        return switch (type) {
            case WARN -> translate(language, CointCoreMessages.PUNISHMENT_TYPE_WARN);
            case MUTE -> translate(language, CointCoreMessages.PUNISHMENT_TYPE_MUTE);
            case UNMUTE -> translate(language, CointCoreMessages.PUNISHMENT_TYPE_UNMUTE);
            case BAN -> translate(language, CointCoreMessages.PUNISHMENT_TYPE_BAN);
            case UNBAN -> translate(language, CointCoreMessages.PUNISHMENT_TYPE_UNBAN);
        };
    }

    public static String formatIssuedAt(long issuedAt) {
        return DATE_TIME.format(Instant.ofEpochMilli(issuedAt).atZone(ZoneId.systemDefault()));
    }

    public static String formatDuration(String language, PunishmentRecord record) {
        if (record.expiresAt() == PunishmentHistory.NO_EXPIRY) {
            return translate(language, CointCoreMessages.PUNISHMENT_DURATION_NA);
        }

        if (record.expiresAt() == PunishmentHistory.PERMANENT) {
            return translate(language, CointCoreMessages.PUNISHMENT_DURATION_PERM);
        }

        long durationMs = Math.max(0L, record.expiresAt() - record.issuedAt());
        return TimeUtil.formatDuration(durationMs);
    }

    private static String translate(String language, String key) {
        String normalized = language == null || language.isBlank()
                ? "en_us"
                : language.toLowerCase(Locale.ROOT).replace('-', '_');
        String text = CointCoreMessages.translateKey(normalized, key);
        if (text != null) {
            return text;
        }

        text = CointCoreMessages.translateKey("en_us", key);
        return text != null ? text : key;
    }
}
