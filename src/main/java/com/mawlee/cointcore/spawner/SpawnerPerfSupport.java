package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.mixin.accessor.BaseSpawnerDelayAccessor;
import com.mawlee.cointcore.mixin.accessor.BaseSpawnerRatesAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Shared helpers for spawner TPS mixins (Apothic + vanilla).
 */
public final class SpawnerPerfSupport {
    private SpawnerPerfSupport() {
    }

    /**
     * Floor Apothic/vanilla rate <em>settings</em> so ultra-fast farms cannot burn a full
     * spawn wave every tick.
     * <p>
     * Must NOT rewrite the live {@code spawnDelay} countdown. Clamping {@code 1..floor-1}
     * back up to {@code floor} every tick (then Apothic does {@code --}) stuck spawners at
     * {@code floor → floor-1} forever — never {@code 0}, never loot/blood/XP.
     */
    public static void clampSpawnRates(BaseSpawner spawner) {
        if (!SpawnerPerfConfig.isEnabled()) {
            return;
        }
        BaseSpawnerRatesAccessor rates = (BaseSpawnerRatesAccessor) spawner;
        int floor = SpawnerPerfConfig.minSpawnDelayFloor();
        if (floor > 0) {
            if (rates.cointcore$getMinSpawnDelay() < floor) {
                rates.cointcore$setMinSpawnDelay(floor);
            }
            if (rates.cointcore$getMaxSpawnDelay() < rates.cointcore$getMinSpawnDelay()) {
                rates.cointcore$setMaxSpawnDelay(rates.cointcore$getMinSpawnDelay());
            }
        }
        int maxCount = SpawnerPerfConfig.maxSpawnCount();
        if (rates.cointcore$getSpawnCount() > maxCount) {
            rates.cointcore$setSpawnCount(maxCount);
        }
    }

    /**
     * After a spawn wave that left {@code spawnDelay == 0} (total failure), force a cooldown
     * so the spawner does not retry the expensive path every tick.
     */
    public static void applyFailedSpawnCooldown(
            BaseSpawner spawner,
            Level level,
            BlockPos pos,
            boolean spawnAttemptedThisTick,
            BiConsumer<Level, BlockPos> delayInvoker
    ) {
        if (!SpawnerPerfConfig.isEnabled() || !SpawnerPerfConfig.forceDelayOnFailedSpawn()) {
            return;
        }
        if (!spawnAttemptedThisTick) {
            return;
        }
        BaseSpawnerDelayAccessor access = (BaseSpawnerDelayAccessor) spawner;
        if (access.cointcore$getSpawnDelay() != 0) {
            return;
        }
        delayInvoker.accept(level, pos);
        int minCooldown = Math.max(SpawnerPerfConfig.minFailCooldownTicks(), SpawnerPerfConfig.minSpawnDelayFloor());
        if (minCooldown > 0 && access.cointcore$getSpawnDelay() < minCooldown) {
            access.cointcore$setSpawnDelay(minCooldown);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List cacheOrScanNearby(
            NearbyScanCache cache,
            Class entityClass,
            AABB aabb,
            java.util.function.Supplier<List> scan
    ) {
        if (!SpawnerPerfConfig.isEnabled() || !SpawnerPerfConfig.cacheNearbyEntityScans()) {
            return scan.get();
        }
        if (cache.list != null && cache.entityClass == entityClass && aabbEquals(cache.aabb, aabb)) {
            return cache.list;
        }
        List result = scan.get();
        cache.entityClass = entityClass;
        cache.aabb = aabb;
        cache.list = result;
        return result;
    }

    private static boolean aabbEquals(AABB a, AABB b) {
        if (a == null || b == null) {
            return false;
        }
        return a.minX == b.minX
                && a.minY == b.minY
                && a.minZ == b.minZ
                && a.maxX == b.maxX
                && a.maxY == b.maxY
                && a.maxZ == b.maxZ;
    }

    public static final class NearbyScanCache {
        public Class<?> entityClass;
        public AABB aabb;
        public List<?> list;

        public void clear() {
            entityClass = null;
            aabb = null;
            list = null;
        }
    }
}
