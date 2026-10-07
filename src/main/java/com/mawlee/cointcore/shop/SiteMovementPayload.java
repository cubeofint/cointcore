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

    static long acceptedUpTo(String json, long fallback) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return root.has("accepted_up_to") ? Math.min(fallback, root.get("accepted_up_to").getAsLong()) : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

}
