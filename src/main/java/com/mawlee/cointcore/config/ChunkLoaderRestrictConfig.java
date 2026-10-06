package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Config-toggled restrictions for mod chunk loaders that lack an ATM10-friendly disable switch.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code chunk_loaders}.
 * Master {@code enabled} must be true; each loader flag defaults false.
 */
public final class ChunkLoaderRestrictConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = false;
    private static boolean disableAe2SpatialAnchor = false;
    private static boolean disableCompactMachinesChunkloader = false;
    private static boolean disableHnnDataCenter = false;
    private static boolean disableIeChunkLoader = false;
    private static boolean disableRailcraftWorldSpike = false;
    private static boolean disableStevesCartsChunkLoader = false;

    private ChunkLoaderRestrictConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDisableAe2SpatialAnchor() {
        return enabled && disableAe2SpatialAnchor;
    }

    public static boolean isDisableCompactMachinesChunkloader() {
        return enabled && disableCompactMachinesChunkloader;
    }

    public static boolean isDisableHnnDataCenter() {
        return enabled && disableHnnDataCenter;
    }

    public static boolean isDisableIeChunkLoader() {
        return enabled && disableIeChunkLoader;
    }

    public static boolean isDisableRailcraftWorldSpike() {
        return enabled && disableRailcraftWorldSpike;
    }

    public static boolean isDisableStevesCartsChunkLoader() {
        return enabled && disableStevesCartsChunkLoader;
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
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded chunk loader restrict (enabled={}, ae2={}, cm={}, hnn={}, ie={}, railcraft={}, steves={})",
                enabled,
                disableAe2SpatialAnchor,
                disableCompactMachinesChunkloader,
                disableHnnDataCenter,
                disableIeChunkLoader,
                disableRailcraftWorldSpike,
                disableStevesCartsChunkLoader
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        disableAe2SpatialAnchor = loaded.disableAe2SpatialAnchor();
        disableCompactMachinesChunkloader = loaded.disableCompactMachinesChunkloader();
        disableHnnDataCenter = loaded.disableHnnDataCenter();
        disableIeChunkLoader = loaded.disableIeChunkLoader();
        disableRailcraftWorldSpike = loaded.disableRailcraftWorldSpike();
        disableStevesCartsChunkLoader = loaded.disableStevesCartsChunkLoader();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                data.disableAe2SpatialAnchor,
                data.disableCompactMachinesChunkloader,
                data.disableHnnDataCenter,
                data.disableIeChunkLoader,
                data.disableRailcraftWorldSpike,
                data.disableStevesCartsChunkLoader
        );
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(
            boolean enabled,
            boolean disableAe2SpatialAnchor,
            boolean disableCompactMachinesChunkloader,
            boolean disableHnnDataCenter,
            boolean disableIeChunkLoader,
            boolean disableRailcraftWorldSpike,
            boolean disableStevesCartsChunkLoader
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = false;

        @SerializedName("disable_ae2_spatial_anchor")
        boolean disableAe2SpatialAnchor = false;

        @SerializedName("disable_compact_machines_chunkloader")
        boolean disableCompactMachinesChunkloader = false;

        @SerializedName("disable_hnn_data_center")
        boolean disableHnnDataCenter = false;

        @SerializedName("disable_ie_chunk_loader")
        boolean disableIeChunkLoader = false;

        @SerializedName("disable_railcraft_world_spike")
        boolean disableRailcraftWorldSpike = false;

        @SerializedName("disable_steves_carts_chunk_loader")
        boolean disableStevesCartsChunkLoader = false;
    }
}
