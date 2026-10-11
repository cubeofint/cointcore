package com.mawlee.cointcore.prefix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pure catalog normalisation, display-name derivation and payload hashing.
 */
public final class PrefixCatalog {
    public static final int SCHEMA = 1;
    public static final String CATEGORY = "quest";
    public static final String META_DISPLAY_NAME = "cointcore-prefix-name";

    private static final Pattern HEX_AMP = Pattern.compile("(?i)&#[0-9a-f]{6}");
    private static final Pattern HEX_X_AMP = Pattern.compile("(?i)&x(?:&[0-9a-f]){6}");
    private static final Pattern HEX_SEC = Pattern.compile("(?i)§x(?:§[0-9a-f]){6}");
    private static final Pattern LEGACY = Pattern.compile("(?i)[&§][0-9a-fk-orx]");
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern BRACKETS = Pattern.compile("[\\[\\]]");
    private static final Pattern WS = Pattern.compile("\\s+");

    private PrefixCatalog() {
    }

    /**
     * {@code meta cointcore-prefix-name} &gt; trimmed displayname node &gt; stripped prefix text &gt; group.
     */
    public static String displayName(String metaName, String displayNameNode, String prefixRaw, String group) {
        String meta = trimToNull(metaName);
        if (meta != null) {
            return meta;
        }
        String display = trimToNull(displayNameNode);
        if (display != null) {
            return display;
        }
        String stripped = trimToNull(stripPrefixText(prefixRaw));
        if (stripped != null) {
            return stripped;
        }
        String fallback = PrefixSnapshot.normalize(group);
        return fallback == null ? "" : fallback;
    }

    public static String stripPrefixText(String prefixRaw) {
        if (prefixRaw == null || prefixRaw.isBlank()) {
            return "";
        }
        String text = HEX_X_AMP.matcher(prefixRaw).replaceAll("");
        text = HEX_SEC.matcher(text).replaceAll("");
        text = HEX_AMP.matcher(text).replaceAll("");
        text = LEGACY.matcher(text).replaceAll("");
        text = TAGS.matcher(text).replaceAll("");
        text = BRACKETS.matcher(text).replaceAll("");
        text = WS.matcher(text).replaceAll(" ").trim();
        return text;
    }

    public static List<PrefixCatalogEntry> normalize(List<PrefixCatalogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<PrefixCatalogEntry> result = new ArrayList<>();
        for (PrefixCatalogEntry entry : entries) {
            if (entry == null || entry.group().isBlank()) {
                continue;
            }
            String display = entry.displayName().isBlank()
                    ? displayName(null, null, entry.prefixRaw(), entry.group())
                    : entry.displayName().trim();
            result.add(new PrefixCatalogEntry(
                    entry.group(),
                    entry.prefixRaw(),
                    entry.priority(),
                    entry.trackIndex(),
                    display,
                    CATEGORY
            ));
        }
        result.sort(Comparator
                .comparingInt(PrefixCatalogEntry::trackIndex)
                .thenComparing(PrefixCatalogEntry::group));
        return List.copyOf(result);
    }

    public static String hash(List<PrefixCatalogEntry> normalized) {
        return sha256Hex(toGroupsArray(normalize(normalized)).toString());
    }

    public static JsonObject toPayload(List<PrefixCatalogEntry> normalized, String hash, boolean full) {
        JsonObject body = new JsonObject();
        body.addProperty("schema", SCHEMA);
        body.addProperty("hash", hash == null ? "" : hash);
        body.addProperty("full", full);
        body.add("groups", toGroupsArray(normalize(normalized)));
        return body;
    }

    public static JsonArray toGroupsArray(List<PrefixCatalogEntry> normalized) {
        JsonArray array = new JsonArray();
        for (PrefixCatalogEntry entry : normalize(normalized)) {
            JsonObject row = new JsonObject();
            row.addProperty("group", entry.group());
            row.addProperty("prefix_raw", entry.prefixRaw());
            row.addProperty("priority", entry.priority());
            row.addProperty("track_index", entry.trackIndex());
            row.addProperty("display_name", entry.displayName());
            row.addProperty("category", CATEGORY);
            array.add(row);
        }
        return array;
    }

    public static List<UUID> sortAfterCursor(Set<UUID> ids, String cursor) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> sorted = new ArrayList<>(ids);
        sorted.sort(Comparator.comparing(UUID::toString));
        if (cursor == null || cursor.isBlank()) {
            return List.copyOf(sorted);
        }
        List<UUID> remaining = new ArrayList<>();
        for (UUID id : sorted) {
            if (id.toString().compareTo(cursor) > 0) {
                remaining.add(id);
            }
        }
        return List.copyOf(remaining);
    }

    static String sha256Hex(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
