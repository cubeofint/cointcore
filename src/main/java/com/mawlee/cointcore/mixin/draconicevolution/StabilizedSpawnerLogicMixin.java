package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.StabilizedSpawnerLogic;
import com.brandon3055.draconicevolution.blocks.tileentity.TileStabilizedSpawner;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.items.MobSoul;
import com.mawlee.cointcore.config.SpawnerByproductConfig;
import com.mawlee.cointcore.config.SpawnerLootConfig;
import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.SpawnerByproductService;
import com.mawlee.cointcore.spawner.SpawnerLootService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stabilized Spawner: with adjacent storage, skip {@code MobSoul.createEntity} /
 * {@code finalizeMobSpawn} and roll loot {@code spawnCount} times on a cached template.
 * Without storage, only reset the timer (no entity construction).
 */
@Mixin(value = StabilizedSpawnerLogic.class, remap = false)
public abstract class StabilizedSpawnerLogicMixin {

    @Shadow
    private TileStabilizedSpawner tile;

    @Shadow
    private void resetTimer() {
    }

    @Unique
    private ResourceLocation cointcore$cachedSoulType;

    @Unique
    private Mob cointcore$cachedMob;

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void cointcore$virtualLoot(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (!SpawnerLootConfig.isEnabled()) {
            return;
        }
        if (!((StabilizedSpawnerLogic) (Object) this).isNearPlayer(level, pos)) {
            return;
        }

        if (tile.spawnDelay.get() == -1) {
            cointcore$resetWithPerfLimits(false);
        }
        if (tile.spawnDelay.get() > 0) {
            tile.spawnDelay.dec();
            ci.cancel();
            return;
        }

        boolean itemSink = SpawnerLootService.hasAdjacentStorageWithSpace(level, pos);
        if (!itemSink) {
            cointcore$resetWithPerfLimits(true);
            ci.cancel();
            return;
        }

        Mob template = cointcore$cachedTemplate(level);
        if (template == null) {
            cointcore$resetWithPerfLimits(true);
            ci.cancel();
            return;
        }

        int spawnCount = cointcore$clampedSpawnCount();
        boolean fluidSink = SpawnerByproductService.hasFluidByproductSink(level, pos);
        for (int i = 0; i < spawnCount; i++) {
            SpawnerLootService.generateAndInsert(level, pos, template);
            if (fluidSink || SpawnerByproductConfig.isEnabled()) {
                SpawnerByproductService.tryDepositAll(level, pos, template);
            }
        }
        cointcore$resetWithPerfLimits(false);
        ci.cancel();
    }

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(ServerLevel level, Entity entity) {
        return MachineSpawnLoot.capturePassengers(level, entity, tile.getBlockPos());
    }

    @Unique
    private Mob cointcore$cachedTemplate(ServerLevel level) {
        ItemStack soulStack = tile.mobSoul.get();
        if (soulStack.isEmpty()) {
            cointcore$discardCache();
            return null;
        }
        MobSoul soul = DEContent.MOB_SOUL.get();
        ResourceLocation typeId = soul.getEntity(soulStack);
        if (cointcore$cachedMob != null
                && typeId.equals(cointcore$cachedSoulType)
                && !cointcore$cachedMob.isRemoved()) {
            cointcore$cachedMob.moveTo(
                    tile.getBlockPos().getX() + 0.5D,
                    tile.getBlockPos().getY() + 0.5D,
                    tile.getBlockPos().getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            return cointcore$cachedMob;
        }

        cointcore$discardCache();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(typeId);
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        mob.moveTo(
                tile.getBlockPos().getX() + 0.5D,
                tile.getBlockPos().getY() + 0.5D,
                tile.getBlockPos().getZ() + 0.5D,
                0.0F,
                0.0F
        );
        cointcore$cachedSoulType = typeId;
        cointcore$cachedMob = mob;
        return mob;
    }

    @Unique
    private void cointcore$discardCache() {
        if (cointcore$cachedMob != null) {
            cointcore$cachedMob.discard();
            cointcore$cachedMob = null;
        }
        cointcore$cachedSoulType = null;
    }

    @Unique
    private int cointcore$clampedSpawnCount() {
        int spawnCount = Math.max(1, tile.spawnerTier.get().getSpawnCount());
        if (SpawnerPerfConfig.isEnabled()) {
            spawnCount = Math.min(spawnCount, SpawnerPerfConfig.maxSpawnCount());
        }
        return Math.max(1, spawnCount);
    }

    @Unique
    private void cointcore$resetWithPerfLimits(boolean failed) {
        resetTimer();
        if (!SpawnerPerfConfig.isEnabled()) {
            return;
        }
        int floor = SpawnerPerfConfig.minSpawnDelayFloor();
        if (failed && SpawnerPerfConfig.forceDelayOnFailedSpawn()) {
            floor = Math.max(floor, SpawnerPerfConfig.minFailCooldownTicks());
        }
        if (floor > 0 && tile.spawnDelay.get() < floor) {
            tile.spawnDelay.set((short) floor);
        }
    }
}
