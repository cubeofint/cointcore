package com.mawlee.cointcore.crowded;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Pure crowded-mob decisions (no Minecraft types) for unit tests.
 */
public final class CrowdedMobPolicy {
    private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private CrowdedMobPolicy() {
    }

    public static Set<String> parseEntityIds(List<String> raw, Consumer<String> invalidId) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String id = entry.trim();
            if (!ENTITY_ID.matcher(id).matches()) {
                if (invalidId != null) {
                    invalidId.accept(entry);
                }
                continue;
            }
            ids.add(id);
        }
        return Set.copyOf(ids);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isTypeAllowed(String typeId, Set<String> allow, Set<String> deny) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        if (deny != null && deny.contains(typeId)) {
            return false;
        }
        if (allow != null && !allow.isEmpty()) {
            return allow.contains(typeId);
        }
        return true;
    }

    public static boolean isEligible(
            boolean player,
            boolean boss,
            boolean villager,
            boolean named,
            boolean leashed,
            boolean includeBosses,
            boolean includeVillagers,
            boolean includeNamed,
            boolean includeLeashed
    ) {
        if (player) {
            return false;
        }
        if (boss && !includeBosses) {
            return false;
        }
        if (villager && !includeVillagers) {
            return false;
        }
        if (named && !includeNamed) {
            return false;
        }
        if (leashed && !includeLeashed) {
            return false;
        }
        return true;
    }

    public static boolean isCrowdedCount(int sameTypeAlive, int threshold) {
        return sameTypeAlive >= Math.max(2, threshold);
    }

    public static boolean shouldRecheck(int tickCount, int nextCheckTick) {
        return tickCount >= nextCheckTick;
    }

    public static int nextCheckTick(int tickCount, int interval) {
        return tickCount + Math.max(1, interval);
    }

    public static boolean shouldSkipAi(
            boolean enabled,
            boolean crowded,
            boolean throttleAi,
            int tickCount,
            int entityId,
            int aiInterval
    ) {
        if (!enabled || !crowded || !throttleAi) {
            return false;
        }
        int interval = Math.max(1, aiInterval);
        return Math.floorMod(tickCount + entityId, interval) != 0;
    }

    public static boolean shouldDisablePush(boolean enabled, boolean crowded, boolean disableEntityPush) {
        return enabled && crowded && disableEntityPush;
    }

    public static boolean shouldSkipAmbient(boolean enabled, boolean crowded, boolean skipAmbientSounds) {
        return enabled && crowded && skipAmbientSounds;
    }
}
