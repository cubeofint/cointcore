package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Server-side throttles for Nature's Aura level-tick / Plant Boost cost.
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code naturesaura}.
 */
public final class NaturesAuraPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_PLANT_BOOST_AMOUNT = 1;
    private static final int MAX_PLANT_BOOST_AMOUNT = 75;

    private static boolean optimizeAuraChunkTick = true;
    private static boolean plantBoostEnabled = true;
    private static int plantBoostMaxAmount = 8;

    private NaturesAuraPerfConfig() {
    }

    public static boolean isOptimizeAuraChunkTick() {
        return optimizeAuraChunkTick;
    }

    public static boolean isPlantBoostEnabled() {
        return plantBoostEnabled;
    }

    public static int getPlantBoostMaxAmount() {
        return plantBoostMaxAmount;
    }

    public static Path getConfigPath() {
        return TickThrottleConfigs.path();
    }

    public static void load() {
        TickThrottleConfigs.load();
    }

    public static boolean reload() {
        return TickThrottleConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded NaturesAura perf config (optimizeAuraChunkTick={}, plantBoostEnabled={}, plantBoostMaxAmount={})",
                optimizeAuraChunkTick,
                plantBoostEnabled,
                plantBoostMaxAmount
        );
    }

    private static void apply(LoadedConfig loaded) {
        optimizeAuraChunkTick = loaded.optimizeAuraChunkTick();
        plantBoostEnabled = loaded.plantBoostEnabled();
        plantBoostMaxAmount = loaded.plantBoostMaxAmount();
    }

    private static LoadedConfig parse(FileData data) {
        boolean optimize = data.optimizeAuraChunkTick == null || data.optimizeAuraChunkTick;
        boolean plantBoost = data.plantBoostEnabled == null || data.plantBoostEnabled;
        int maxAmount = clamp(
                data.plantBoostMaxAmount != null ? data.plantBoostMaxAmount : 8,
                MIN_PLANT_BOOST_AMOUNT,
                MAX_PLANT_BOOST_AMOUNT
        );
        return new LoadedConfig(optimize, plantBoost, maxAmount);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.optimizeAuraChunkTick = true;
        data.plantBoostEnabled = true;
        data.plantBoostMaxAmount = 8;
        return data;
    }

    private record LoadedConfig(boolean optimizeAuraChunkTick, boolean plantBoostEnabled, int plantBoostMaxAmount) {
    }

    static final class FileData {
        @SerializedName("optimizeAuraChunkTick")
        private Boolean optimizeAuraChunkTick;

        @SerializedName("plantBoostEnabled")
        private Boolean plantBoostEnabled;

        @SerializedName("plantBoostMaxAmount")
        private Integer plantBoostMaxAmount;
    }
}
