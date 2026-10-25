package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * File: {@code config/cointcore/cataclysm-respawn.json} section {@code draconic_chaos_guardian}.
 * Re-arms Draconic Evolution chaos guardian fights on every server start.
 */
public final class ChaosGuardianRespawnConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static int delayTicks = 200;
    private static int maxRadiusBlocks = 30000;
    private static boolean replaceBlocks = false;

    private ChaosGuardianRespawnConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Ticks after server start before islands are checked. */
    public static int getDelayTicks() {
        return delayTicks;
    }

    /** Islands farther than this from 0 0 (or outside the End world border) are ignored. */
    public static int getMaxRadiusBlocks() {
        return maxRadiusBlocks;
    }

    /** Whether a non-air block standing where the chaos crystal was may be replaced by the crystal. */
    public static boolean isReplaceBlocks() {
        return replaceBlocks;
    }

    static void applySection(FileData data) {
        FileData source = data != null ? data : defaultFileData();
        enabled = source.enabled != null ? source.enabled : true;
        delayTicks = source.delayTicks != null && source.delayTicks >= 0 ? source.delayTicks : 200;
        maxRadiusBlocks = source.maxRadiusBlocks != null && source.maxRadiusBlocks > 0 ? source.maxRadiusBlocks : 30000;
        replaceBlocks = source.replaceBlocks != null && source.replaceBlocks;
    }

    static void logReload() {
        LOGGER.info("Reloaded chaos guardian respawn config (enabled={}, maxRadius={})", enabled, maxRadiusBlocks);
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.delayTicks = 200;
        data.maxRadiusBlocks = 30000;
        data.replaceBlocks = false;
        return data;
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("delayTicks")
        Integer delayTicks;

        @SerializedName("maxRadiusBlocks")
        Integer maxRadiusBlocks;

        @SerializedName("replaceBlocks")
        Boolean replaceBlocks;
    }
}
