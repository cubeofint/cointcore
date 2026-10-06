package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File: {@code config/cointcore/cataclysm-respawn.json} section {@code sunken_city}.
 */
public final class SunkenCityRespawnConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static int tickInterval = 1200;
    private static int chunkScanRadius = 8;
    private static int playerActivationBlocks = 128;
    private static int maxSpawnsPerCyclePerType = 1;
    private static int spawnPositionAttempts = 24;
    private static int localCountRadius = 48;
    private static int localMaxPerType = 3;
    private static int spawnMinDistanceFromAltar = 10;
    private static int spawnMaxDistanceFromAltar = 36;
    private static int minDistanceFromPlayer = 8;
    /** Game ticks after a tracked mob dies before that type can refill (0 = no delay). Default 6000 ≈ 5 min. */
    private static int respawnDelayTicks = 6000;
    private static boolean persistentRespawns = false;
    private static ResourceLocation altarBlockId = ResourceLocation.fromNamespaceAndPath("cataclysm", "altar_of_abyss");
    private static Map<ResourceLocation, Integer> mobTargets = Map.of();

    private SunkenCityRespawnConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getTickInterval() {
        return tickInterval;
    }

    public static int getChunkScanRadius() {
        return chunkScanRadius;
    }

    public static int getPlayerActivationBlocks() {
        return playerActivationBlocks;
    }

    public static int getMaxSpawnsPerCyclePerType() {
        return maxSpawnsPerCyclePerType;
    }

    public static int getSpawnPositionAttempts() {
        return spawnPositionAttempts;
    }

    public static int getLocalCountRadius() {
        return localCountRadius;
    }

    public static int getLocalMaxPerType() {
        return localMaxPerType;
    }

    public static int getSpawnMinDistanceFromAltar() {
        return spawnMinDistanceFromAltar;
    }

    public static int getSpawnMaxDistanceFromAltar() {
        return spawnMaxDistanceFromAltar;
    }

    public static int getMinDistanceFromPlayer() {
        return minDistanceFromPlayer;
    }

    public static int getRespawnDelayTicks() {
        return respawnDelayTicks;
    }

    public static boolean isPersistentRespawns() {
        return persistentRespawns;
    }

    public static ResourceLocation getAltarBlockId() {
        return altarBlockId;
    }

    public static Map<ResourceLocation, Integer> getMobTargets() {
        return mobTargets;
    }

    public static Path getConfigPath() {
        return CataclysmRespawnConfigs.path();
    }

    public static void load() {
        CataclysmRespawnConfigs.load();
    }

    public static boolean reload() {
        return CataclysmRespawnConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded sunken city respawn config ({} targets, interval={}, localMax={})",
                mobTargets.size(),
                tickInterval,
                localMaxPerType
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled;
        tickInterval = loaded.tickInterval;
        chunkScanRadius = loaded.chunkScanRadius;
        playerActivationBlocks = loaded.playerActivationBlocks;
        maxSpawnsPerCyclePerType = loaded.maxSpawnsPerCyclePerType;
        spawnPositionAttempts = loaded.spawnPositionAttempts;
        localCountRadius = loaded.localCountRadius;
        localMaxPerType = loaded.localMaxPerType;
        spawnMinDistanceFromAltar = loaded.spawnMinDistanceFromAltar;
        spawnMaxDistanceFromAltar = loaded.spawnMaxDistanceFromAltar;
        minDistanceFromPlayer = loaded.minDistanceFromPlayer;
        respawnDelayTicks = loaded.respawnDelayTicks;
        persistentRespawns = loaded.persistentRespawns;
        altarBlockId = loaded.altarBlockId;
        mobTargets = loaded.mobTargets;
    }

    private static LoadedConfig parse(FileData data) {
        Map<ResourceLocation, Integer> targets = new LinkedHashMap<>();
        if (data.mobTargets != null) {
            for (Map.Entry<String, Integer> entry : data.mobTargets.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() <= 0) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey().trim());
                if (id == null) {
                    LOGGER.warn("Skipping invalid sunken city mob target id: {}", entry.getKey());
                    continue;
                }
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    LOGGER.warn("Unknown sunken city mob target entity type: {}", id);
                }
                targets.put(id, entry.getValue());
            }
        }

        if (targets.isEmpty()) {
            targets = defaultMobTargets();
        }

        ResourceLocation altarId = ResourceLocation.fromNamespaceAndPath("cataclysm", "altar_of_abyss");
        if (data.altarBlockId != null && !data.altarBlockId.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(data.altarBlockId.trim());
            if (parsed != null) {
                altarId = parsed;
            } else {
                LOGGER.warn("Invalid sunken city altarBlockId '{}', using cataclysm:altar_of_abyss", data.altarBlockId);
            }
        }

        int minAltar = positiveOrDefault(data.spawnMinDistanceFromAltar, 10);
        int maxAltar = positiveOrDefault(data.spawnMaxDistanceFromAltar, 36);
        if (maxAltar < minAltar) {
            maxAltar = minAltar;
        }

        return new LoadedConfig(
                data.enabled != null ? data.enabled : true,
                positiveOrDefault(data.tickInterval, 1200),
                positiveOrDefault(data.chunkScanRadius, 8),
                positiveOrDefault(data.playerActivationBlocks, 128),
                positiveOrDefault(data.maxSpawnsPerCyclePerType, 1),
                positiveOrDefault(data.spawnPositionAttempts, 24),
                positiveOrDefault(data.localCountRadius, 48),
                positiveOrDefault(data.localMaxPerType, 3),
                minAltar,
                maxAltar,
                positiveOrDefault(data.minDistanceFromPlayer, 8),
                nonNegativeOrDefault(data.respawnDelayTicks, 6000),
                data.persistentRespawns != null ? data.persistentRespawns : false,
                altarId,
                Map.copyOf(targets)
        );
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private static int nonNegativeOrDefault(Integer value, int defaultValue) {
        return value != null && value >= 0 ? value : defaultValue;
    }

    private static Map<ResourceLocation, Integer> defaultMobTargets() {
        Map<ResourceLocation, Integer> defaults = new LinkedHashMap<>();
        // Structure-wide hard caps (not per-player bubble).
        putTarget(defaults, "cataclysm:deepling", 8);
        putTarget(defaults, "cataclysm:deepling_angler", 4);
        putTarget(defaults, "cataclysm:deepling_brute", 3);
        putTarget(defaults, "cataclysm:deepling_priest", 2);
        putTarget(defaults, "cataclysm:deepling_warlock", 2);
        // coralssus: spot-respawn via CataclysmStructureRespawnService
        putTarget(defaults, "cataclysm:coral_golem", 2);
        return Map.copyOf(defaults);
    }

    private static void putTarget(Map<ResourceLocation, Integer> targets, String id, int count) {
        targets.put(ResourceLocation.parse(id), count);
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.tickInterval = 1200;
        data.chunkScanRadius = 8;
        data.playerActivationBlocks = 128;
        data.maxSpawnsPerCyclePerType = 1;
        data.spawnPositionAttempts = 24;
        data.localCountRadius = 48;
        data.localMaxPerType = 3;
        data.spawnMinDistanceFromAltar = 10;
        data.spawnMaxDistanceFromAltar = 36;
        data.minDistanceFromPlayer = 8;
        data.respawnDelayTicks = 6000;
        data.persistentRespawns = false;
        data.altarBlockId = "cataclysm:altar_of_abyss";
        data.mobTargets = new LinkedHashMap<>();
        defaultMobTargets().forEach((id, count) -> data.mobTargets.put(id.toString(), count));
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int tickInterval,
            int chunkScanRadius,
            int playerActivationBlocks,
            int maxSpawnsPerCyclePerType,
            int spawnPositionAttempts,
            int localCountRadius,
            int localMaxPerType,
            int spawnMinDistanceFromAltar,
            int spawnMaxDistanceFromAltar,
            int minDistanceFromPlayer,
            int respawnDelayTicks,
            boolean persistentRespawns,
            ResourceLocation altarBlockId,
            Map<ResourceLocation, Integer> mobTargets
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("tickInterval")
        Integer tickInterval;

        @SerializedName("chunkScanRadius")
        Integer chunkScanRadius;

        @SerializedName("playerActivationBlocks")
        Integer playerActivationBlocks;

        @SerializedName("maxSpawnsPerCyclePerType")
        Integer maxSpawnsPerCyclePerType;

        @SerializedName("spawnPositionAttempts")
        Integer spawnPositionAttempts;

        @SerializedName("localCountRadius")
        Integer localCountRadius;

        @SerializedName("localMaxPerType")
        Integer localMaxPerType;

        @SerializedName("spawnMinDistanceFromAltar")
        Integer spawnMinDistanceFromAltar;

        @SerializedName("spawnMaxDistanceFromAltar")
        Integer spawnMaxDistanceFromAltar;

        @SerializedName("minDistanceFromPlayer")
        Integer minDistanceFromPlayer;

        @SerializedName("respawnDelayTicks")
        Integer respawnDelayTicks;

        @SerializedName("persistentRespawns")
        Boolean persistentRespawns;

        @SerializedName("altarBlockId")
        String altarBlockId;

        @SerializedName("mobTargets")
        Map<String, Integer> mobTargets;
    }
}
