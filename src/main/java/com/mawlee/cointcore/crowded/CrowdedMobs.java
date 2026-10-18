package com.mawlee.cointcore.crowded;

import com.mawlee.cointcore.config.CrowdedMobsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-thread crowded-farm tracker. Density uses the level entity section lookup
 * with a same-type cap, not a full world scan.
 */
public final class CrowdedMobs {
    private static int flagged;

    private CrowdedMobs() {
    }

    public static boolean isCrowded(Entity entity) {
        return entity instanceof CrowdedMobMarker marker && marker.cointcore$isCrowded();
    }

    public static void refresh(Mob mob, CrowdedMobMarker marker) {
        if (mob.level().isClientSide() || !CrowdedMobsConfig.isEnabled()) {
            clearIfNeeded(marker);
            return;
        }
        int tick = mob.tickCount;
        if (!CrowdedMobPolicy.shouldRecheck(tick, marker.cointcore$nextCrowdedCheckTick())) {
            return;
        }
        boolean crowded = computeCrowded(mob);
        updateFlagged(marker.cointcore$isCrowded(), crowded);
        marker.cointcore$setCrowded(crowded);
        marker.cointcore$setNextCrowdedCheckTick(
                CrowdedMobPolicy.nextCheckTick(tick, CrowdedMobsConfig.recheckIntervalTicks())
        );
    }

    public static boolean shouldSkipAi(Mob mob, CrowdedMobMarker marker) {
        return CrowdedMobPolicy.shouldSkipAi(
                CrowdedMobsConfig.isEnabled(),
                marker.cointcore$isCrowded(),
                CrowdedMobsConfig.throttleAi(),
                mob.tickCount,
                mob.getId(),
                CrowdedMobsConfig.aiIntervalTicks()
        );
    }

    public static boolean shouldDisablePush(Entity entity) {
        return CrowdedMobPolicy.shouldDisablePush(
                CrowdedMobsConfig.isEnabled(),
                isCrowded(entity),
                CrowdedMobsConfig.disableEntityPush()
        );
    }

    public static boolean shouldSkipPushBetween(Entity first, Entity second) {
        return shouldDisablePush(first) && shouldDisablePush(second);
    }

    public static boolean shouldSkipAmbient(Mob mob, CrowdedMobMarker marker) {
        return CrowdedMobPolicy.shouldSkipAmbient(
                CrowdedMobsConfig.isEnabled(),
                marker.cointcore$isCrowded(),
                CrowdedMobsConfig.skipAmbientSounds()
        );
    }

    public static void onRemove(Entity entity) {
        if (entity instanceof CrowdedMobMarker marker && marker.cointcore$isCrowded()) {
            flagged = Math.max(0, flagged - 1);
            marker.cointcore$setCrowded(false);
        }
    }

    public static int flaggedCount() {
        return flagged;
    }

    public static String statusLine() {
        return String.format(
                Locale.ROOT,
                "Crowded mobs: enabled=%s flagged=%d threshold=%d radius=%.2f recheck=%d ai=%d",
                CrowdedMobsConfig.isEnabled(),
                flagged,
                CrowdedMobsConfig.sameTypeThreshold(),
                CrowdedMobsConfig.radiusBlocks(),
                CrowdedMobsConfig.recheckIntervalTicks(),
                CrowdedMobsConfig.aiIntervalTicks()
        );
    }

    public static void reset() {
        flagged = 0;
    }

    private static void clearIfNeeded(CrowdedMobMarker marker) {
        if (marker.cointcore$isCrowded()) {
            updateFlagged(true, false);
            marker.cointcore$setCrowded(false);
        }
        marker.cointcore$setNextCrowdedCheckTick(0);
    }

    private static void updateFlagged(boolean was, boolean now) {
        if (was == now) {
            return;
        }
        if (now) {
            flagged++;
        } else {
            flagged = Math.max(0, flagged - 1);
        }
    }

    private static boolean computeCrowded(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !eligible(mob)) {
            return false;
        }
        int threshold = CrowdedMobsConfig.sameTypeThreshold();
        double radius = CrowdedMobsConfig.radiusBlocks();
        AABB box = mob.getBoundingBox().inflate(radius);
        List<Mob> found = new ArrayList<>(threshold);
        level.getEntities(
                EntityTypeTest.forClass(Mob.class),
                box,
                candidate -> candidate.getType() == mob.getType() && candidate.isAlive(),
                found,
                threshold
        );
        return CrowdedMobPolicy.isCrowdedCount(found.size(), threshold);
    }

    private static boolean eligible(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String typeId = id == null ? "" : id.toString();
        if (!CrowdedMobPolicy.isTypeAllowed(
                typeId,
                CrowdedMobsConfig.allowEntityTypes(),
                CrowdedMobsConfig.denyEntityTypes()
        )) {
            return false;
        }
        boolean boss = mob.getType().is(Tags.EntityTypes.BOSSES);
        boolean villager = mob instanceof AbstractVillager;
        boolean named = mob.hasCustomName();
        boolean leashed = mob.isLeashed();
        return CrowdedMobPolicy.isEligible(
                false,
                boss,
                villager,
                named,
                leashed,
                CrowdedMobsConfig.includeBosses(),
                CrowdedMobsConfig.includeVillagers(),
                CrowdedMobsConfig.includeNamed(),
                CrowdedMobsConfig.includeLeashed()
        );
    }
}
