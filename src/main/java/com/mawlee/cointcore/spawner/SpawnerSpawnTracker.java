package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Pending farm-spawner intercepts between {@link net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent}
 * and add-to-world. Also covers mobs that skip {@code Mob#finalizeSpawn} super (null spawn type).
 */
public final class SpawnerSpawnTracker {
    private static final ThreadLocal<Map<Mob, BlockPos>> PENDING =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private SpawnerSpawnTracker() {
    }

    public static void markFarmSpawn(Mob mob, BlockPos spawnerPos) {
        // Do not clear other pending entries — Apothic spawnCount can finalize several
        // mobs before each joins, and wiping would drop loot/blood/XP for earlier ones.
        PENDING.get().put(mob, spawnerPos.immutable());
    }

    public static BlockPos consumeFarmSpawn(Mob mob) {
        return PENDING.get().remove(mob);
    }

    public static boolean hasFarmSpawn(Mob mob) {
        return PENDING.get().containsKey(mob);
    }
}
