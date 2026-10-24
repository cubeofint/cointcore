package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two entity costs that grow with players and loaded farms.
 *
 * <ul>
 *     <li>{@code entity_tracking_throttle}: every movement packet made vanilla re-check every entity of the level for
 *     that player (3.6% of the tick at ~1800 entities, 13 players). Do the full pass only after the player moved
 *     {@code min_move_blocks} or {@code max_ticks} passed; entities crossing sections are still handled every tick
 *     by {@code ChunkMap.tick}.</li>
 *     <li>{@code far_mob_ai}: mobs with no player within {@code distance} run their AI step (sensing, goals,
 *     navigation, move/look control) once every {@code interval} ticks; physics, damage and death stay every tick.
 *     Types in {@code exclude} (id or {@code namespace:*}) are never slowed.</li>
 * </ul>
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} sections {@code entity_tracking_throttle} and {@code far_mob_ai}.
 */
public final class EntityPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean trackingEnabled = true;
    private static double trackingMinMoveSq = 64.0D;
    private static int trackingMaxTicks = 20;

    private static boolean farAiEnabled = true;
    private static double farAiDistanceSq = 48.0D * 48.0D;
    private static int farAiInterval = 4;
    private static List<String> farAiExcludeIds = List.of();
    private static List<String> farAiExcludeNamespaces = List.of();
    private static final Map<EntityType<?>, Boolean> EXCLUDED = new ConcurrentHashMap<>();

    private EntityPerfConfig() {
    }

    public static boolean trackingEnabled() {
        return trackingEnabled;
    }

    public static double trackingMinMoveSq() {
        return trackingMinMoveSq;
    }

    public static int trackingMaxTicks() {
        return trackingMaxTicks;
    }

    public static boolean farAiEnabled() {
        return farAiEnabled;
    }

    public static double farAiDistanceSq() {
        return farAiDistanceSq;
    }

    public static int farAiInterval() {
        return farAiInterval;
    }

    public static boolean farAiExcluded(EntityType<?> type) {
        return EXCLUDED.computeIfAbsent(type, EntityPerfConfig::resolveExcluded);
    }

    private static boolean resolveExcluded(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        return farAiExcludeIds.contains(id.toString()) || farAiExcludeNamespaces.contains(id.getNamespace());
    }

    static void applyTracking(TrackingData data) {
        TrackingData resolved = data != null ? data : new TrackingData();
        trackingEnabled = resolved.enabled;
        double move = Math.max(1, Math.min(64, resolved.minMoveBlocks));
        trackingMinMoveSq = move * move;
        trackingMaxTicks = Math.max(1, Math.min(200, resolved.maxTicks));
    }

    static void applyFarAi(FarAiData data) {
        FarAiData resolved = data != null ? data : new FarAiData();
        farAiEnabled = resolved.enabled;
        double distance = Math.max(8, Math.min(256, resolved.distance));
        farAiDistanceSq = distance * distance;
        farAiInterval = Math.max(1, Math.min(20, resolved.interval));
        List<String> ids = new ArrayList<>();
        List<String> namespaces = new ArrayList<>();
        if (resolved.exclude != null) {
            for (String raw : resolved.exclude) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String value = raw.trim().toLowerCase(Locale.ROOT);
                if (value.endsWith(":*")) {
                    namespaces.add(value.substring(0, value.length() - 2));
                } else {
                    ids.add(value);
                }
            }
        }
        farAiExcludeIds = List.copyOf(ids);
        farAiExcludeNamespaces = List.copyOf(namespaces);
        EXCLUDED.clear();
    }

    static void logReload() {
        LOGGER.info("Reloaded entity perf (tracking={} move={} ticks={}, farMobAi={} dist={} every={} exclude={}+{})",
                trackingEnabled, Math.sqrt(trackingMinMoveSq), trackingMaxTicks, farAiEnabled,
                Math.sqrt(farAiDistanceSq), farAiInterval, farAiExcludeIds, farAiExcludeNamespaces);
    }

    static final class TrackingData {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Full visibility pass after the player moved this far since the last one. */
        @SerializedName("min_move_blocks")
        int minMoveBlocks = 8;

        /** ...or after this many ticks, whichever comes first. */
        @SerializedName("max_ticks")
        int maxTicks = 20;
    }

    static final class FarAiData {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Mobs with no player closer than this (blocks) are slowed. */
        @SerializedName("distance")
        int distance = 48;

        /** Slowed mobs think once every this many ticks. */
        @SerializedName("interval")
        int interval = 4;

        /** Never slowed: entity ids or "namespace:*". Automation and colony mobs by default. */
        @SerializedName("exclude")
        List<String> exclude = new ArrayList<>(List.of(
                "minecraft:villager",
                "minecraft:iron_golem",
                "minecraft:bee",
                "minecraft:allay",
                "productivebees:*",
                "ars_nouveau:*",
                "minecolonies:*"
        ));
    }
}
