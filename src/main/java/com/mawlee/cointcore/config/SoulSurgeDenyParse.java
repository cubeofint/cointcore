package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Minecraft-free parse of {@code config/cointcore/soul-surge-deny.json}.
 * Extra keys (including leftover {@code denyBlockNamespaces}) are ignored.
 */
public final class SoulSurgeDenyParse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SoulSurgeDenyParse() {
    }

    public static Parsed parseJson(String json) {
        FileData data = GSON.fromJson(json, FileData.class);
        return parse(data);
    }

    static Parsed parse(FileData data) {
        FileData source = data != null ? data : new FileData();
        Set<String> ids = new LinkedHashSet<>();
        if (source.denyBlockIds != null) {
            for (String raw : source.denyBlockIds) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                ids.add(raw.trim());
            }
        }
        List<String> tags = new ArrayList<>();
        Set<String> seenTags = new LinkedHashSet<>();
        if (source.denyBlockTags != null) {
            for (String raw : source.denyBlockTags) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.startsWith("#")) {
                    trimmed = trimmed.substring(1);
                }
                if (trimmed.isBlank() || !seenTags.add(trimmed)) {
                    continue;
                }
                tags.add(trimmed);
            }
        }
        return new Parsed(Set.copyOf(ids), List.copyOf(tags));
    }

    static Gson gson() {
        return GSON;
    }

    public record Parsed(Set<String> denyBlockIds, List<String> denyBlockTags) {
        public boolean containsId(String blockId) {
            return blockId != null && denyBlockIds.contains(blockId);
        }
    }

    static final class FileData {
        @SerializedName("denyBlockIds")
        List<String> denyBlockIds;

        @SerializedName("denyBlockTags")
        List<String> denyBlockTags;

        static FileData defaults() {
            FileData data = new FileData();
            data.denyBlockIds = List.of();
            data.denyBlockTags = List.of();
            return data;
        }
    }
}
