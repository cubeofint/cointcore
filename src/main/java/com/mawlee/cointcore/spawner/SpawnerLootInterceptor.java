package com.mawlee.cointcore.spawner;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

public final class SpawnerLootInterceptor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int successLogBudget = 8;

    private SpawnerLootInterceptor() {
    }

    public enum HandleResult {
        NOT_APPLICABLE,
        CONSUMED
    }

    /**
     * Consumes a pending farm-spawner spawn: deposits loot/byproducts and blocks the mob.
     */
    public static HandleResult handle(ServerLevel level, Mob mob, boolean discardEntity) {
        BlockPos lootSpawner = SpawnerSpawnTracker.consumeFarmSpawn(mob);
        if (lootSpawner == null) {
            return HandleResult.NOT_APPLICABLE;
        }

        // Always simulate loot — do not gate on "has space" (filters/sided caps used to skip entirely).
        SpawnerLootService.generateAndInsert(level, lootSpawner, mob);
        SpawnerByproductService.tryDepositAll(level, lootSpawner, mob);

        if (successLogBudget > 0) {
            successLogBudget--;
            LOGGER.info(
                    "Farm spawner converted {} at {} (loot/byproducts deposited or dropped)",
                    mob.getType().getDescriptionId(),
                    lootSpawner
            );
        }

        if (discardEntity) {
            mob.discard();
        }

        return HandleResult.CONSUMED;
    }
}
