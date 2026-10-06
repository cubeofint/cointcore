package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerByproductConfig;
import com.mawlee.cointcore.mixin.accessor.BaseSpawnerRatesAccessor;
import com.mawlee.cointcore.mixin.accessor.BaseSpawnerSpawnDataInvoker;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.slf4j.Logger;

/**
 * Cheap farm-spawner harvest: no collision / position checks, one mob per wave.
 * Full sinks → void the wave without entity/loot work (overflow is never grounded).
 */
public final class SpawnerFarmHarvest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int waveLogBudget = 8;

    private SpawnerFarmHarvest() {
    }

    /**
     * @return {@code true} if this tick's spawn wave was fully handled (caller should
     *         invoke {@code delay} and skip the vanilla/Apothic spawn loop)
     */
    public static boolean tryHarvestWave(ServerLevel level, BlockPos spawnerPos, BaseSpawner spawner) {
        if (!PlayerSpawnerService.isFarmSpawner(level, spawnerPos)) {
            return false;
        }

        PlayerSpawnerService.ensureIndexedIfFarm(level, level.getBlockEntity(spawnerPos));

        boolean itemSink = SpawnerLootService.hasAdjacentStorageWithSpace(level, spawnerPos);
        boolean fluidSink = SpawnerByproductService.hasFluidByproductSink(level, spawnerPos);
        if (!itemSink && !fluidSink) {
            // Chest/tanks full (or absent): void the wave — no entity, no loot roll.
            logWave(spawnerPos, "void-full", 0, 0);
            return true;
        }

        int spawnCount = Math.max(1, ((BaseSpawnerRatesAccessor) spawner).cointcore$getSpawnCount());
        RandomSource random = level.getRandom();
        SpawnData spawnData = ((BaseSpawnerSpawnDataInvoker) spawner)
                .cointcore$getOrCreateNextSpawnData(level, random, spawnerPos);
        CompoundTag tag = spawnData.getEntityToSpawn();
        EntityType<?> type = EntityType.by(tag).orElse(null);
        if (type == null) {
            return true;
        }

        Mob mob = createTemplateMob(level, spawnerPos, tag, type);
        if (mob == null) {
            return true;
        }

        try {
            for (int i = 0; i < spawnCount; i++) {
                if (itemSink) {
                    SpawnerLootService.generateAndInsert(level, spawnerPos, mob);
                }
                if (fluidSink || (SpawnerByproductConfig.isEnabled() && itemSink)) {
                    SpawnerByproductService.tryDepositAll(level, spawnerPos, mob);
                }
            }
            logWave(spawnerPos, type.getDescriptionId(), spawnCount, spawnCount);
        } finally {
            mob.discard();
        }

        return true;
    }

    private static Mob createTemplateMob(
            ServerLevel level,
            BlockPos spawnerPos,
            CompoundTag tag,
            EntityType<?> type
    ) {
        double x = spawnerPos.getX() + 0.5D;
        double y = spawnerPos.getY() + 0.5D;
        double z = spawnerPos.getZ() + 0.5D;

        Entity entity;
        // SpawnData with only "id" — cheap create; otherwise preserve NBT (equipment etc.).
        if (tag.size() == 1 && tag.contains("id", CompoundTag.TAG_STRING)) {
            entity = type.create(level);
            if (entity != null) {
                entity.moveTo(x, y, z, 0.0F, 0.0F);
            }
        } else {
            entity = EntityType.loadEntityRecursive(tag, level, fresh -> {
                fresh.moveTo(x, y, z, fresh.getYRot(), fresh.getXRot());
                return fresh;
            });
        }

        if (entity instanceof Mob mob) {
            return mob;
        }
        if (entity != null) {
            entity.discard();
        }
        return null;
    }

    private static void logWave(BlockPos spawnerPos, String type, int requested, int converted) {
        if (waveLogBudget <= 0) {
            return;
        }
        waveLogBudget--;
        LOGGER.info(
                "Farm spawner wave at {}: type={}, requested={}, converted={}",
                spawnerPos,
                type,
                requested,
                converted
        );
    }
}
