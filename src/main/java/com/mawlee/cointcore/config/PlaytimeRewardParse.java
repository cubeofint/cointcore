package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Minecraft-free parse of {@code config/cointcore/playtime-reward.json}.
 */
public final class PlaytimeRewardParse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final long DEFAULT_AMOUNT = 4L;
    public static final int DEFAULT_INTERVAL_MINUTES = 60;

    private PlaytimeRewardParse() {
    }

    public static Parsed parseJson(String json) {
        return parse(GSON.fromJson(json, FileData.class));
    }

    static Parsed parse(FileData data) {
        FileData source = data != null ? data : FileData.defaults();
        boolean enabled = source.enabled == null || source.enabled;
        long amount = source.amount == null ? DEFAULT_AMOUNT : Math.max(0L, source.amount);
        int intervalMinutes = source.intervalMinutes == null
                ? DEFAULT_INTERVAL_MINUTES
                : Math.max(1, Math.min(source.intervalMinutes, 7 * 24 * 60));
        return new Parsed(enabled, amount, intervalMinutes);
    }

    static Gson gson() {
        return GSON;
    }

    public record Parsed(boolean enabled, long amount, int intervalMinutes) {
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("amount")
        Long amount;

        @SerializedName("intervalMinutes")
        Integer intervalMinutes;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.amount = DEFAULT_AMOUNT;
            data.intervalMinutes = DEFAULT_INTERVAL_MINUTES;
            return data;
        }
    }
}
