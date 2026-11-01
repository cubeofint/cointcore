package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

/** Optional Mekanism hot-path optimizations in storage-perf.json. */
public final class MekanismPerfConfig {
    private static boolean directItemContainerLookup = true;

    private MekanismPerfConfig() { }

    public static boolean isDirectItemContainerLookup() { return directItemContainerLookup; }

    static void applySection(Data data) {
        directItemContainerLookup = data == null || data.directItemContainerLookup;
    }

    static Data defaultFileData() { return new Data(); }

    static final class Data {
        @SerializedName("direct_item_container_lookup")
        boolean directItemContainerLookup = true;
    }
}
