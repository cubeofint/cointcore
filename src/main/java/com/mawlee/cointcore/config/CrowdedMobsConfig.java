package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import com.mawlee.cointcore.crowded.CrowdedMobPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Throttles entity-push and AI for packed same-type mob farms.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code crowded_mobs}.
 * Defaults to {@code enabled=false}.
 */
public final class CrowdedMobsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 256;
    private static final double MIN_RADIUS = 0.25D;
    private static final double MAX_RADIUS = 8.0D;
    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 200;

    private static boolean enabled = false;
    private static int sameTypeThreshold = 8;
    private static double radiusBlocks = 1.0D;
    private static int recheckIntervalTicks = 30;
    private static int aiIntervalTicks = 15;
    private static boolean disableEntityPush = true;
    private static boolean throttleAi = true;
    private static boolean skipAmbientSounds = true;
    private static boolean includeVillagers = false;
    private static boolean includeNamed = false;
    private static boolean includeLeashed = false;
    private static boolean includeBosses = false;
    private static Set<String> allowEntityTypes = Set.of();
    private static Set<String> denyEntityTypes = Set.of();

    private CrowdedMobsConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int sameTypeThreshold() {
        return sameTypeThreshold;
    }

    public static double radiusBlocks() {
        return radiusBlocks;
    }

    public static int recheckIntervalTicks() {
        return recheckIntervalTicks;
    }

    public static int aiIntervalTicks() {
        return aiIntervalTicks;
    }

    public static boolean disableEntityPush() {
        return disableEntityPush;
    }

    public static boolean throttleAi() {
        return throttleAi;
    }

    public static boolean skipAmbientSounds() {
        return skipAmbientSounds;
    }

    public static boolean includeVillagers() {
        return includeVillagers;
    }

    public static boolean includeNamed() {
        return includeNamed;
    }

    public static boolean includeLeashed() {
        return includeLeashed;
    }

    public static boolean includeBosses() {
        return includeBosses;
    }

    public static Set<String> allowEntityTypes() {
        return allowEntityTypes;
    }

    public static Set<String> denyEntityTypes() {
        return denyEntityTypes;
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
                "Reloaded crowded_mobs (enabled={}, threshold={}, radius={}, recheck={}, ai={}, push={}, "
                        + "villagers={}, named={}, leashed={}, bosses={}, allow={}, deny={})",
                enabled,
                sameTypeThreshold,
                radiusBlocks,
                recheckIntervalTicks,
                aiIntervalTicks,
                disableEntityPush,
                includeVillagers,
                includeNamed,
                includeLeashed,
                includeBosses,
                allowEntityTypes.size(),
                denyEntityTypes.size()
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        sameTypeThreshold = loaded.sameTypeThreshold();
        radiusBlocks = loaded.radiusBlocks();
        recheckIntervalTicks = loaded.recheckIntervalTicks();
        aiIntervalTicks = loaded.aiIntervalTicks();
        disableEntityPush = loaded.disableEntityPush();
        throttleAi = loaded.throttleAi();
        skipAmbientSounds = loaded.skipAmbientSounds();
        includeVillagers = loaded.includeVillagers();
        includeNamed = loaded.includeNamed();
        includeLeashed = loaded.includeLeashed();
        includeBosses = loaded.includeBosses();
        allowEntityTypes = loaded.allowEntityTypes();
        denyEntityTypes = loaded.denyEntityTypes();
    }

    private static LoadedConfig parse(Data data) {
        return new LoadedConfig(
                data.enabled,
                CrowdedMobPolicy.clamp(data.sameTypeThreshold, MIN_THRESHOLD, MAX_THRESHOLD),
                CrowdedMobPolicy.clamp(data.radiusBlocks, MIN_RADIUS, MAX_RADIUS),
                CrowdedMobPolicy.clamp(data.recheckIntervalTicks, MIN_INTERVAL, MAX_INTERVAL),
                CrowdedMobPolicy.clamp(data.aiIntervalTicks, MIN_INTERVAL, MAX_INTERVAL),
                data.disableEntityPush,
                data.throttleAi,
                data.skipAmbientSounds,
                data.includeVillagers,
                data.includeNamed,
                data.includeLeashed,
                data.includeBosses,
                CrowdedMobPolicy.parseEntityIds(
                        data.allowEntityTypes,
                        id -> LOGGER.warn("Ignoring invalid crowded_mobs allow id '{}'", id)
                ),
                CrowdedMobPolicy.parseEntityIds(
                        data.denyEntityTypes,
                        id -> LOGGER.warn("Ignoring invalid crowded_mobs deny id '{}'", id)
                )
        );
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(
            boolean enabled,
            int sameTypeThreshold,
            double radiusBlocks,
            int recheckIntervalTicks,
            int aiIntervalTicks,
            boolean disableEntityPush,
            boolean throttleAi,
            boolean skipAmbientSounds,
            boolean includeVillagers,
            boolean includeNamed,
            boolean includeLeashed,
            boolean includeBosses,
            Set<String> allowEntityTypes,
            Set<String> denyEntityTypes
    ) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = false;

        @SerializedName("same_type_threshold")
        int sameTypeThreshold = 8;

        @SerializedName("radius_blocks")
        double radiusBlocks = 1.0D;

        @SerializedName("recheck_interval_ticks")
        int recheckIntervalTicks = 30;

        @SerializedName("ai_interval_ticks")
        int aiIntervalTicks = 15;

        @SerializedName("disable_entity_push")
        boolean disableEntityPush = true;

        @SerializedName("throttle_ai")
        boolean throttleAi = true;

        @SerializedName("skip_ambient_sounds")
        boolean skipAmbientSounds = true;

        @SerializedName("include_villagers")
        boolean includeVillagers = false;

        @SerializedName("include_named")
        boolean includeNamed = false;

        @SerializedName("include_leashed")
        boolean includeLeashed = false;

        @SerializedName("include_bosses")
        boolean includeBosses = false;

        @SerializedName("allow_entity_types")
        List<String> allowEntityTypes = new ArrayList<>();

        @SerializedName("deny_entity_types")
        List<String> denyEntityTypes = new ArrayList<>();
    }
}
