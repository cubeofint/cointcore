package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;

public final class IntegratedTransferPerfConfig {
    private static boolean emptySourceFastPath = true;
    private IntegratedTransferPerfConfig() { }
    public static boolean isEmptySourceFastPath() { return emptySourceFastPath; }
    static void applySection(Data data) { emptySourceFastPath = data == null || data.emptySourceFastPath; }
    static Data defaultFileData() { return new Data(); }
    static final class Data {
        @SerializedName("empty_source_fast_path")
        boolean emptySourceFastPath = true;
    }
}
