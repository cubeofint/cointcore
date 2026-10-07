package com.mawlee.cointcore.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** One queued site&lt;-&gt;server transfer. {@code toServer} credits the wallet, otherwise debits it. */
public record SiteOperation(String id, UUID playerId, String playerName, long amount, boolean toServer) {
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
                if (uuid == null || amount <= 0
                        || !("to_server".equals(direction) || "from_server".equals(direction))) {
                    malformed.accept(id);
                    continue;
                }
                result.add(new SiteOperation(id, uuid, name == null ? "" : name, amount, "to_server".equals(direction)));
            } catch (RuntimeException e) {
                malformed.accept(id);
            }
        }
        return result;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
