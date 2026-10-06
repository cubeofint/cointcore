package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Spot-based refill + far-despawn for Cataclysm structure-only elites
 * (jigsaw / template slots; 1 mob per spot, per-spot cooldown).
 *
 * <p>File: {@code config/cointcore/cataclysm-respawn.json} section {@code structures}.
 */
public final class CataclysmStructureRespawnConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static int tickInterval = 600;
    private static int chunkScanRadius = 8;
    private static int playerActivationBlocks = 96;
    private static int maxSpawnsPerCycle = 3;
    /** Soft safety cap of living tracked mobs counted inside one structure BB. */
    private static int structureMax = 64;
    private static int spawnMinDistanceFromPlayer = 6;
    /** Per-spot cooldown after spawn/death. Default 12000 ≈ 10 min. */
    private static int respawnDelayTicks = 12000;
    private static int despawnDistance = 64;
    private static boolean despawnEnabled = true;
    /** How often to run far-despawn scans (ticks). Default 100 ≈ 5s (was hardcoded 20). */
    private static int despawnCheckInterval = 100;
    private static Set<ResourceLocation> structures = Set.of();
    private static Set<ResourceLocation> mobIds = Set.of();
    private static Set<EntityType<?>> trackedEntityTypes = Set.of();
    private static boolean trackedTypesDirty = true;

    private CataclysmStructureRespawnConfig() {
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

    public static int getMaxSpawnsPerCycle() {
        return maxSpawnsPerCycle;
    }

    public static int getStructureMax() {
        return structureMax;
    }

    public static int getSpawnMinDistanceFromPlayer() {
        return spawnMinDistanceFromPlayer;
    }

    public static int getRespawnDelayTicks() {
        return respawnDelayTicks;
    }

    public static int getDespawnDistance() {
        return despawnDistance;
    }

    public static boolean isDespawnEnabled() {
        return despawnEnabled;
    }

    public static int getDespawnCheckInterval() {
        return despawnCheckInterval;
    }

    public static Set<ResourceLocation> getStructures() {
        return structures;
    }

    public static Set<ResourceLocation> getMobIds() {
        return mobIds;
    }

    public static boolean tracksMob(ResourceLocation id) {
        return id != null && mobIds.contains(id);
    }

    public static boolean tracksMobType(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        ensureTrackedEntityTypes();
        return trackedEntityTypes.contains(type);
    }

    public static Set<EntityType<?>> getTrackedEntityTypes() {
        ensureTrackedEntityTypes();
        return trackedEntityTypes;
    }

    public static boolean tracksStructure(ResourceLocation id) {
        return id != null && structures.contains(id);
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
                "Reloaded cataclysm structure respawn (mobs={}, structures={}, spotCd={})",
                mobIds.size(),
                structures.size(),
                respawnDelayTicks
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled;
        tickInterval = loaded.tickInterval;
        chunkScanRadius = loaded.chunkScanRadius;
        playerActivationBlocks = loaded.playerActivationBlocks;
        maxSpawnsPerCycle = loaded.maxSpawnsPerCycle;
        structureMax = loaded.structureMax;
        spawnMinDistanceFromPlayer = loaded.spawnMinDistanceFromPlayer;
        respawnDelayTicks = loaded.respawnDelayTicks;
        despawnDistance = loaded.despawnDistance;
        despawnEnabled = loaded.despawnEnabled;
        despawnCheckInterval = loaded.despawnCheckInterval;
        structures = loaded.structures;
        mobIds = loaded.mobIds;
        trackedTypesDirty = true;
        trackedEntityTypes = Set.of();
    }

    private static void ensureTrackedEntityTypes() {
        if (!trackedTypesDirty) {
            return;
        }
        Set<EntityType<?>> types = new HashSet<>();
        for (ResourceLocation id : mobIds) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(types::add);
        }
        trackedEntityTypes = Set.copyOf(types);
        trackedTypesDirty = false;
    }

    private static LoadedConfig parse(FileData data) {
        Set<ResourceLocation> structIds = parseIdList(data.structures, defaultStructures(), "structure");
        Set<ResourceLocation> trackedMobs = parseIdList(data.mobIds, defaultMobIds(), "mob");

        return new LoadedConfig(
                data.enabled != null ? data.enabled : true,
                positiveOrDefault(data.tickInterval, 600),
                positiveOrDefault(data.chunkScanRadius, 8),
                positiveOrDefault(data.playerActivationBlocks, 96),
                positiveOrDefault(data.maxSpawnsPerCycle, 3),
                positiveOrDefault(data.structureMax, 64),
                nonNegativeOrDefault(data.spawnMinDistanceFromPlayer, 6),
                nonNegativeOrDefault(data.respawnDelayTicks, 12000),
                positiveOrDefault(data.despawnDistance, 64),
                data.despawnEnabled != null ? data.despawnEnabled : true,
                positiveOrDefault(data.despawnCheckInterval, 100),
                Set.copyOf(structIds),
                Set.copyOf(trackedMobs)
        );
    }

    private static Set<ResourceLocation> parseIdList(List<String> raw, List<String> defaults, String label) {
        List<String> source = raw == null || raw.isEmpty() ? defaults : raw;
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (String entry : source) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid cataclysm structure-respawn {} id: {}", label, entry);
                continue;
            }
            out.add(id);
        }
        if (out.isEmpty()) {
            for (String entry : defaults) {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private static int nonNegativeOrDefault(Integer value, int defaultValue) {
        return value != null && value >= 0 ? value : defaultValue;
    }

    private static List<String> defaultStructures() {
        return List.of(
                "cataclysm:frosted_prison",
                "cataclysm:abandoned_spire",
                "cataclysm:abandoned_temple",
                "cataclysm:abandoned_village",
                "cataclysm:desert_temple",
                "cataclysm:desert_site",
                "cataclysm:desert_occupied_village",
                "cataclysm:cursed_pyramid",
                "cataclysm:acropolis",
                "cataclysm:ancient_factory",
                "cataclysm:soul_black_smith",
                "cataclysm:sunken_city"
        );
    }

    private static List<String> defaultMobIds() {
        return List.of(
                "cataclysm:aptrgangr",
                "cataclysm:draugr",
                "cataclysm:elite_draugr",
                "cataclysm:royal_draugr",
                "cataclysm:kobolediator",
                "cataclysm:wadjet",
                "cataclysm:cindaria",
                "cataclysm:clawdian",
                "cataclysm:hippocamtus",
                "cataclysm:urchinkin",
                "cataclysm:scylla",
                "cataclysm:drowned_host",
                "cataclysm:symbiocto",
                "cataclysm:the_watcher",
                "cataclysm:the_prowler",
                "cataclysm:the_harbinger",
                "cataclysm:netherite_ministrosity",
                "cataclysm:netherite_monstrosity",
                "cataclysm:coralssus"
        );
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.tickInterval = 600;
        data.chunkScanRadius = 8;
        data.playerActivationBlocks = 96;
        data.maxSpawnsPerCycle = 3;
        data.structureMax = 64;
        data.spawnMinDistanceFromPlayer = 6;
        data.respawnDelayTicks = 12000;
        data.despawnDistance = 64;
        data.despawnEnabled = true;
        data.despawnCheckInterval = 100;
        data.structures = new ArrayList<>(defaultStructures());
        data.mobIds = new ArrayList<>(defaultMobIds());
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            int tickInterval,
            int chunkScanRadius,
            int playerActivationBlocks,
            int maxSpawnsPerCycle,
            int structureMax,
            int spawnMinDistanceFromPlayer,
            int respawnDelayTicks,
            int despawnDistance,
            boolean despawnEnabled,
            int despawnCheckInterval,
            Set<ResourceLocation> structures,
            Set<ResourceLocation> mobIds
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

        @SerializedName("maxSpawnsPerCycle")
        Integer maxSpawnsPerCycle;

        @SerializedName("structureMax")
        Integer structureMax;

        @SerializedName("spawnMinDistanceFromPlayer")
        Integer spawnMinDistanceFromPlayer;

        @SerializedName("respawnDelayTicks")
        Integer respawnDelayTicks;

        @SerializedName("despawnDistance")
        Integer despawnDistance;

        @SerializedName("despawnEnabled")
        Boolean despawnEnabled;

        @SerializedName("despawnCheckInterval")
        Integer despawnCheckInterval;

        @SerializedName("structures")
        List<String> structures;

        @SerializedName("mobIds")
        List<String> mobIds;
    }
}
