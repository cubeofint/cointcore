package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

public final class ModularBeesRsConfig {
    private static boolean enabled = true;
    private static int interval = 100;
    private ModularBeesRsConfig() { }

    public static boolean isEnabled() { return enabled; }
    public static int getIntervalTicks() { return interval; }

    static Data defaultFileData() { return new Data(); }
    static void applySection(Data data) {
        var value = data == null ? defaultFileData() : data;
        enabled = value.enabled;
        interval = Math.max(1, Math.min(1200, value.interval));
    }

    static final class Data {
        @SerializedName("enabled") boolean enabled = true;
        @SerializedName("transfer_interval_ticks") int interval = 100;
    }
}
