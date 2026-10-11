package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Minecraft-free parse of the deprecated {@code soul-surge-deny.json}.
 * Old deny lists are accepted and ignored.
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
        List<String> ids = source.denyBlockIds != null ? List.copyOf(source.denyBlockIds) : List.of();
        List<String> namespaces = source.denyBlockNamespaces != null
                ? List.copyOf(source.denyBlockNamespaces)
                : List.of();
        List<String> tags = source.denyBlockTags != null ? List.copyOf(source.denyBlockTags) : List.of();
        return new Parsed(
                Boolean.TRUE.equals(source.deprecated) || Boolean.TRUE.equals(source.unused),
                ids,
                namespaces,
                tags
        );
    }

    static Gson gson() {
        return GSON;
    }

    public record Parsed(
            boolean deprecated,
            List<String> denyBlockIds,
            List<String> denyBlockNamespaces,
            List<String> denyBlockTags
    ) {
    }

    static final class FileData {
        @SerializedName("deprecated")
        Boolean deprecated;

        @SerializedName("unused")
        Boolean unused;

        @SerializedName("denyBlockIds")
        List<String> denyBlockIds;

        @SerializedName("denyBlockNamespaces")
        List<String> denyBlockNamespaces;

        @SerializedName("denyBlockTags")
        List<String> denyBlockTags;

        static FileData stub() {
            FileData data = new FileData();
            data.deprecated = true;
            data.unused = true;
            data.denyBlockIds = List.of();
            data.denyBlockNamespaces = List.of();
            data.denyBlockTags = List.of();
            return data;
        }
    }
}
