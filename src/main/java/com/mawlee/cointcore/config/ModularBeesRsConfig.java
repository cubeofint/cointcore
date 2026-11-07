package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

public final class ModularBeesRsConfig {
    public static final int DEFAULT_IMPORT_BUFFER_STACKS = 64;
    private static boolean enabled = true;
    private static int interval = 100;
    private static int importBufferStacks = DEFAULT_IMPORT_BUFFER_STACKS;
    private ModularBeesRsConfig() { }

    public static boolean isEnabled() { return enabled; }
    public static int getIntervalTicks() { return interval; }
    /** How many stacks of each configured item an ME import hatch keeps (ModularBees' own value is 8). */
    public static int getImportBufferStacks() { return importBufferStacks; }

    static Data defaultFileData() { return new Data(); }
    static void applySection(Data data) {
        var value = data == null ? defaultFileData() : data;
        enabled = value.enabled;
        interval = Math.max(1, Math.min(1200, value.interval));
        importBufferStacks = Math.max(1, Math.min(1024, value.importBufferStacks));
    }

    static final class Data {
        @SerializedName("enabled") boolean enabled = true;
        @SerializedName("transfer_interval_ticks") int interval = 100;
        @SerializedName("import_buffer_stacks") int importBufferStacks = DEFAULT_IMPORT_BUFFER_STACKS;
    }
}
