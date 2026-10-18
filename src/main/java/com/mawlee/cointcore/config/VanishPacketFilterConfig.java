package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Cheap early-outs for vanish world-packet hiding (sounds / block events / hits).
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code vanish_packet_filter}.
 */
public final class VanishPacketFilterConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_DISTANCE = 8;
    private static final int MAX_DISTANCE = 256;

    private static int maxDistance = 48;

    private VanishPacketFilterConfig() {
    }

    public static int getMaxDistance() {
        return maxDistance;
    }

    public static double getMaxDistanceSq() {
        return (double) maxDistance * (double) maxDistance;
    }

    public static Path getConfigPath() {
        return LagFixesConfigs.path();
    }

    public static void load() {
        LagFixesConfigs.load();
    }

    public static boolean reload() {
        return LagFixesConfigs.reload();
    }

    static void applySection(Data data) {
        Data resolved = data != null ? data : defaultFileData();
        maxDistance = clamp(resolved.maxDistance, MIN_DISTANCE, MAX_DISTANCE);
    }

    static void logReload() {
        LOGGER.info("Reloaded vanish packet filter config (max_distance={})", maxDistance);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("max_distance")
        int maxDistance = 48;
    }
}
