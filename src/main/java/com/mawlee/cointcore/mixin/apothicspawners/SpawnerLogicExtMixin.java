package com.mawlee.cointcore.mixin.apothicspawners;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.mixin.accessor.BaseSpawnerSpawnRangeAccessor;
import com.mawlee.cointcore.spawner.PlayerSpawnerService;
import com.mawlee.cointcore.spawner.SpawnerFarmHarvest;
import com.mawlee.cointcore.spawner.SpawnerLootInterceptor;
import com.mawlee.cointcore.spawner.SpawnerPerfSupport;
import com.mawlee.cointcore.spawner.SpawnerSpawnTracker;
import com.mawlee.cointcore.spawner.VanillaSpawnerLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Apothic spawner TPS + ensure farm-spawn marking before tryAdd
 * (FinalizeSpawn can miss LyingLevel / priority races).
 */
@Mixin(
        targets = "dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile$SpawnerLogicExt",
        remap = true
)
public abstract class SpawnerLogicExtMixin {
    @Shadow
    private void delay(Level level, BlockPos pos) {
    }

    @Unique
    private boolean cointcore$spawnAttempted;

    @Unique
    private boolean cointcore$farmWaveHandled;

    @Unique
    private final SpawnerPerfSupport.NearbyScanCache cointcore$nearbyCache = new SpawnerPerfSupport.NearbyScanCache();

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cointcore$beginTick(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        cointcore$spawnAttempted = false;
        cointcore$farmWaveHandled = false;
        cointcore$nearbyCache.clear();
        BaseSpawnerSpawnRangeAccessor spawner = (BaseSpawnerSpawnRangeAccessor) (Object) this;
        if (spawner.cointcore$getSpawnRange() > VanillaSpawnerLimits.SPAWN_RANGE) {
            spawner.cointcore$setSpawnRange(VanillaSpawnerLimits.SPAWN_RANGE);
        }
        SpawnerPerfSupport.clampSpawnRates((BaseSpawner) (Object) this);
    }

    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getRandom()Lnet/minecraft/util/RandomSource;"
            )
    )
    private RandomSource cointcore$markSpawnAttempt(
            ServerLevel level,
            Operation<RandomSource> original,
            ServerLevel tickLevel,
            BlockPos spawnerPos
    ) {
        if (SpawnerPerfConfig.isEnabled()) {
            cointcore$spawnAttempted = true;
        }
        // Farm wave starts here (Apothic just entered spawnDelay==0). Harvest without
        // noCollision / position checks — otherwise frames/chests around the cage
        // make tryAdd unreachable and the log stays silent.
        if (!cointcore$farmWaveHandled
                && SpawnerFarmHarvest.tryHarvestWave(tickLevel, spawnerPos, (BaseSpawner) (Object) this)) {
            cointcore$farmWaveHandled = true;
            this.delay(tickLevel, spawnerPos);
        }
        return original.call(level);
    }

    /**
     * After farm harvest, abort Apothic's spawn loop (entityType == null → delay; return).
     */
    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;by(Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Optional;"
            )
    )
    private Optional<EntityType<?>> cointcore$abortApothicLoopAfterFarmHarvest(
            CompoundTag tag,
            Operation<Optional<EntityType<?>>> original
    ) {
        if (cointcore$farmWaveHandled) {
            return Optional.empty();
        }
        return original.call(tag);
    }

    /**
     * Skip Apothic's for-loop body after a farm harvest (still lets the method finish).
     */
    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean cointcore$convertFarmBeforeAdd(
            ServerLevel level,
            Entity entity,
            Operation<Boolean> original,
            ServerLevel tickLevel,
            BlockPos spawnerPos
    ) {
        if (cointcore$farmWaveHandled) {
            if (entity instanceof Mob mob) {
                SpawnerSpawnTracker.consumeFarmSpawn(mob);
            }
            if (entity != null) {
                entity.discard();
            }
            return true;
        }
        if (entity instanceof Mob mob && PlayerSpawnerService.isFarmSpawner(level, spawnerPos)) {
            BlockEntity be = level.getBlockEntity(spawnerPos);
            if (be != null) {
                PlayerSpawnerService.ensureIndexedIfFarm(level, be);
            }
            SpawnerSpawnTracker.markFarmSpawn(mob, spawnerPos);
            if (SpawnerLootInterceptor.handle(level, mob, true) == SpawnerLootInterceptor.HandleResult.CONSUMED) {
                return true;
            }
        }
        return original.call(level, entity);
    }

    @WrapOperation(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List cointcore$cacheNearbyScanServer(
            ServerLevel level,
            Class entityClass,
            AABB aabb,
            Operation<List> original
    ) {
        return SpawnerPerfSupport.cacheOrScanNearby(
                cointcore$nearbyCache,
                entityClass,
                aabb,
                () -> original.call(level, entityClass, aabb)
        );
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cointcore$failedSpawnCooldown(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        SpawnerPerfSupport.applyFailedSpawnCooldown(
                (BaseSpawner) (Object) this,
                level,
                pos,
                cointcore$spawnAttempted,
                this::delay
        );
    }
}
