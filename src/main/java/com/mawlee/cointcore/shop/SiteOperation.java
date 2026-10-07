package com.mawlee.cointcore.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** One queued site operation against the in-game wallet. */
public record SiteOperation(
        String id,
        UUID playerId,
        String playerName,
        long amount,
        Kind kind,
        String reason,
        String source
) {
    public static final int REASON_MAX_LENGTH = 190;

    public enum Kind {
        TO_SERVER,
        FROM_SERVER,
        ADJUST
    }

    /** {@code true} for legacy {@code to_server} credits. */
    public boolean toServer() {
        return kind == Kind.TO_SERVER;
    }

    public static UUID parseGameId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() == 32) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16)
                    + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{8,64}");
    }

    /** Parses the site queue payload; operations with a valid id but bad fields go to {@code malformed}. */
    public static List<SiteOperation> parseAll(String json, Consumer<String> malformed) {
        List<SiteOperation> result = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray array = root.has("operations") ? root.getAsJsonArray("operations") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject o = element.getAsJsonObject();
            String id = o.has("id") ? o.get("id").getAsString() : null;
            if (!validId(id)) {
                continue;
            }
            try {
                UUID uuid = parseGameId(str(o, "game_id"));
                long amount = o.get("amount").getAsBigDecimal().longValueExact();
                String direction = str(o, "direction");
                String name = str(o, "name");
                String reason = clipReason(str(o, "reason"));
                String source = str(o, "source");
                if (source == null) {
                    source = "";
                }
                Kind kind = parseKind(direction);
                if (uuid == null || kind == null || !validAmount(kind, amount, source)) {
                    malformed.accept(id);
                    continue;
                }
                result.add(new SiteOperation(id, uuid, name == null ? "" : name, amount, kind, reason, source));
            } catch (RuntimeException e) {
                malformed.accept(id);
            }
        }
        return result;
    }

    static Kind parseKind(String direction) {
        if ("to_server".equals(direction)) {
            return Kind.TO_SERVER;
        }
        if ("from_server".equals(direction)) {
            return Kind.FROM_SERVER;
        }
        if ("adjust".equals(direction)) {
            return Kind.ADJUST;
        }
        return null;
    }

    static boolean validAmount(Kind kind, long amount, String source) {
        return switch (kind) {
            case TO_SERVER, FROM_SERVER -> amount > 0L;
            case ADJUST -> amount != 0L || "reconcile".equals(source);
        };
    }

    static String clipReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.length() <= REASON_MAX_LENGTH ? reason : reason.substring(0, REASON_MAX_LENGTH);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
