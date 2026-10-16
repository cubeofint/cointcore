package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Minecraft-free parse of {@code config/cointcore/reserved-slots.json}.
 */
public final class ReservedSlotsParse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int DEFAULT_PUBLIC_SLOTS = 30;
    public static final int DEFAULT_RESERVED_SLOTS = 10;

    private ReservedSlotsParse() {
    }

    public static Parsed parseJson(String json) {
        return parse(GSON.fromJson(json, FileData.class));
    }

    static Parsed parse(FileData data) {
        FileData source = data != null ? data : FileData.defaults();
        boolean enabled = source.enabled == null || source.enabled;
        int publicSlots = clampSlots(source.publicSlots, DEFAULT_PUBLIC_SLOTS);
        int reservedSlots = clampSlots(source.reservedSlots, DEFAULT_RESERVED_SLOTS);
        return new Parsed(enabled, publicSlots, reservedSlots);
    }

    static Gson gson() {
        return GSON;
    }

    private static int clampSlots(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(value, 10_000));
    }

    public record Parsed(boolean enabled, int publicSlots, int reservedSlots) {
        public int totalSlots() {
            return publicSlots + reservedSlots;
        }
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("publicSlots")
        Integer publicSlots;

        @SerializedName("reservedSlots")
        Integer reservedSlots;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.publicSlots = DEFAULT_PUBLIC_SLOTS;
            data.reservedSlots = DEFAULT_RESERVED_SLOTS;
            return data;
        }
    }
}
