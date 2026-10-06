package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.CointCore;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class SpawnerLootEvents {
    private SpawnerLootEvents() {
    }

    @SubscribeEvent
    public static void onSpawnerPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(Blocks.SPAWNER)) {
            return;
        }

        PlayerSpawnerSavedData.get(level.getServer()).mark(level, event.getPos());
    }

    @SubscribeEvent
    public static void onSpawnerBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(Blocks.SPAWNER)) {
            return;
        }

        PlayerSpawnerSavedData.get(level.getServer()).unmark(level, event.getPos());
    }

    /**
     * Spawn egg on a spawner counts as "touched" even if Apothic has not set {@code modified} yet.
     */
    @SubscribeEvent
    public static void onSpawnEggUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getItemStack().getItem() instanceof SpawnEggItem)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)) {
            return;
        }

        PlayerSpawnerSavedData.get(level.getServer()).mark(level, pos);
    }

    /**
     * Cheap farm decision: only the spawner from NeoForge's event, no chunk BE scans.
     * Untouched dungeon spawners are skipped → normal mob spawn.
     * <p>
     * Apothic may pass {@code LyingLevel} (not a {@link ServerLevel}) into this event when
     * ignore-light is enabled — resolve via {@link net.minecraft.world.level.ServerLevelAccessor#getLevel()}.
     * <p>
     * True when this finalize is a farm (player/Apothic) spawner spawn that CointCore will
     * convert to loot/blood/XP and cancel on join — chunk mob limits must not cancel it first.
     */
    public static boolean isFarmSpawnerFinalize(FinalizeSpawnEvent event) {
        // Do not gate on isSpawnCancelled — ChunkLimit may cancel first on a race;
        // farm detection must still succeed so limits skip the loot path.
        if (event.getSpawnType() != MobSpawnType.SPAWNER) {
            return false;
        }

        BlockEntity spawnerBe = resolveSpawnerBlockEntity(event.getSpawner());
        if (spawnerBe == null) {
            return false;
        }

        ServerLevel level = resolveServerLevel(event, spawnerBe);
        return level != null && PlayerSpawnerService.isFarmSpawner(level, spawnerBe);
    }

    /**
     * Mark at HIGHEST (same band as ChunkLimit) so farm spawns are indexed before
     * any cancel. Apothic also converts in SpawnerLogicExtMixin around tryAdd.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!isFarmSpawnerFinalize(event) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        BlockEntity spawnerBe = resolveSpawnerBlockEntity(event.getSpawner());
        if (spawnerBe == null) {
            return;
        }

        ServerLevel level = resolveServerLevel(event, spawnerBe);
        if (level == null) {
            return;
        }

        PlayerSpawnerService.ensureIndexedIfFarm(level, spawnerBe);
        SpawnerSpawnTracker.markFarmSpawn(mob, spawnerBe.getBlockPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!SpawnerSpawnTracker.hasFarmSpawn(mob)) {
            return;
        }

        if (SpawnerLootInterceptor.handle(level, mob, false) == SpawnerLootInterceptor.HandleResult.CONSUMED) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!SpawnerLootCapture.isActive() || event.getDrops().isEmpty()) {
            return;
        }

        SpawnerLootCapture.captureItemEntities(event.getDrops());
        event.getDrops().clear();
    }

    private static BlockEntity resolveSpawnerBlockEntity(Either<BlockEntity, Entity> spawner) {
        if (spawner == null) {
            return null;
        }

        return spawner.left().filter(blockEntity -> blockEntity instanceof SpawnerBlockEntity).orElse(null);
    }

    private static ServerLevel resolveServerLevel(FinalizeSpawnEvent event, BlockEntity spawnerBe) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            return serverLevel;
        }

        if (event.getLevel() != null) {
            ServerLevel fromAccessor = event.getLevel().getLevel();
            if (fromAccessor != null) {
                return fromAccessor;
            }
        }

        if (spawnerBe.getLevel() instanceof ServerLevel fromSpawner) {
            return fromSpawner;
        }

        if (event.getEntity().level() instanceof ServerLevel fromEntity) {
            return fromEntity;
        }

        return null;
    }
}
