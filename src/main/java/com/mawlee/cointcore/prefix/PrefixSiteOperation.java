package com.mawlee.cointcore.prefix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mawlee.cointcore.shop.SiteOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Site → game {@code prefix_select} operation. Contract kept exact for AzLink-mods / Azuriom.
 *
 * <p>{@code {op_id, uuid, group|null}} delivered as {@code {"operations":[...]}} or a raw array.
 */
public record PrefixSiteOperation(String opId, UUID playerId, String group) {
    public static List<PrefixSiteOperation> parseAll(String json, Consumer<String> malformed) {
        List<PrefixSiteOperation> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        JsonElement root = JsonParser.parseString(json);
        JsonArray array;
        if (root.isJsonArray()) {
            array = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().has("operations")) {
            array = root.getAsJsonObject().getAsJsonArray("operations");
        } else {
            return result;
        }
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String type = str(object, "type");
            if (type != null && !type.isBlank() && !"prefix_select".equalsIgnoreCase(type)) {
                continue;
            }
            String id = first(object, "op_id", "id");
            if (!SiteOperation.validId(id)) {
                continue;
            }
            try {
                UUID uuid = SiteOperation.parseGameId(first(object, "uuid", "game_id"));
                if (uuid == null) {
                    malformed.accept(id);
                    continue;
                }
                String group = first(object, "group", "selected");
                if (group != null && (group.isBlank() || "auto".equalsIgnoreCase(group))) {
                    group = null;
                }
                result.add(new PrefixSiteOperation(id, uuid, PrefixSnapshot.normalize(group)));
            } catch (RuntimeException ignored) {
                malformed.accept(id);
            }
        }
        return result;
    }

    private static String first(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = str(object, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String str(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }
}
