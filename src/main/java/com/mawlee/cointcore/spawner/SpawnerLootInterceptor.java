package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

public final class SpawnerLootInterceptor {
    private SpawnerLootInterceptor() {
    }

    public enum HandleResult {
        NOT_APPLICABLE,
        CONSUMED
    }

    public static HandleResult handle(ServerLevel level, Mob mob, boolean discardEntity) {
        if (mob.getSpawnType() != MobSpawnType.SPAWNER) {
            return HandleResult.NOT_APPLICABLE;
        }

        SpawnerResolution resolution = PlayerSpawnerService.resolve(level, mob);
        if (resolution == null) {
            return HandleResult.NOT_APPLICABLE;
        }

        BlockPos lootSpawner = resolution.lootSpawner();
        if (lootSpawner != null && SpawnerLootService.hasAdjacentStorageWithSpace(level, lootSpawner)) {
            SpawnerLootService.generateAndInsert(level, lootSpawner, mob);
        }

        if (discardEntity) {
            mob.discard();
        }

        return HandleResult.CONSUMED;
    }
}
