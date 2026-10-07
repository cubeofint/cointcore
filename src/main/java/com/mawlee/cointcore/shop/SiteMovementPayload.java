package com.mawlee.cointcore.shop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Pure helpers for {@link SiteMovementSender} (no Minecraft classes, unit-testable). */
final class SiteMovementPayload {
    static final long MIN_BACKOFF_MS = 5_000L;
    static final long MAX_BACKOFF_MS = 300_000L;

    private SiteMovementPayload() {
    }

    static long nextBackoff(long current) {
        return current <= 0 ? MIN_BACKOFF_MS : Math.min(MAX_BACKOFF_MS, current * 2);
    }

    /**
     * Highest movement id the site confirmed, capped by the last id we sent.
     * {@code null} means no confirmation — the outbox must stay unsent and retry.
     */
    static Long acceptedUpTo(String json, long batchLastId) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root == null || !root.has("accepted_up_to") || root.get("accepted_up_to").isJsonNull()) {
                return null;
            }
            long accepted = root.get("accepted_up_to").getAsLong();
            if (accepted <= 0L) {
                return null;
            }
            return Math.min(batchLastId, accepted);
        } catch (RuntimeException e) {
            return null;
        }
    }

}
