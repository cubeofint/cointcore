package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.cataclysm.CataclysmStructureRespawnService;
import com.mawlee.cointcore.cataclysm.SunkenCityRespawnService;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.spawner.SpawnerLootEvents;
import com.mawlee.cointcore.spawner.SpawnerSpawnTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ChunkLimitEvents {
    private ChunkLimitEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled()) {
            return;
        }
        if (ChunkLimitConfig.hasAnyBlockLimits()) {
            ChunkLimitIndex.rebuildChunk(level, chunk);
        }
        PlayerBlockLimitService.validateChunk(level, chunk);
        if (ChunkLimitConfig.hasAnyEntityLimits()) {
            // Defer until entities in the chunk are available after load.
            level.getServer().execute(() -> {
                MobLimitService.cullOverLimit(level, chunk.getPos());
                MobLimitService.syncTeamIndex(level, chunk.getPos());
            });
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkLimitIndex.removeChunk(level, event.getChunk().getPos());
        // Team mob contributions retained across unload (same as block TeamLimitIndex).
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // FakePlayer never bypasses — machine placers respect the cap; item returns to FakePlayer inventory.
        if (!ChunkLimitConfig.isEnabled()
                || (!(player instanceof FakePlayer) && ChunkLimitService.canBypass(player))) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        Block block = placedState.getBlock();
        if (!ChunkLimitConfig.isLimitedBlock(block)) {
            return;
        }

        // Block is already in the world/index when this fires.
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        if (!ChunkLimitService.wouldExceedBlockLimit(player, level, chunkPos, block, 0)) {
            return;
        }

        event.setCanceled(true);
        ChunkLimitService.denyBlockPlacement(player, placedState, chunkPos);
    }

    /** Runs last so only placements that actually stay in the world get an owner. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlacedRecordOwner(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !ChunkLimitConfig.isEnabled()
                || !ChunkLimitConfig.hasPlayerBlockLimits()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
                BlockPos pos = snapshot.getPos();
                PlayerBlockLimitService.recordPlacement(player, level, pos, level.getBlockState(pos));
            }
            return;
        }
        PlayerBlockLimitService.recordPlacement(player, level, event.getPos(), event.getPlacedBlock());
    }

    /**
     * Preempt BlockItem placement when the chunk/team is already at cap, so the player
     * cannot get a block down through paths that skip or mishandle EntityPlaceEvent.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockItemUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled()
                || (!(player instanceof FakePlayer) && ChunkLimitService.canBypass(player))) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        Block block = blockItem.getBlock();
        if (!ChunkLimitConfig.isLimitedBlock(block)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos clicked = event.getPos();
        Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
        BlockPos placePos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        ChunkPos chunkPos = new ChunkPos(placePos);

        if (!ChunkLimitService.wouldExceedBlockLimit(player, level, chunkPos, block, 1)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        ChunkLimitService.notifyBlockPlacementDenied(player, block.defaultBlockState(), chunkPos, 1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || ChunkLimitService.canBypass(player)) {
            return;
        }

        ItemEntity itemEntity = event.getEntity();
        if (!MobLimitService.shouldEnforce(itemEntity)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = itemEntity.chunkPosition();
        if (!MobLimitService.wouldExceed(level, chunkPos, itemEntity)) {
            return;
        }

        event.setCanceled(true);
        ChunkLimitService.denyItemToss(player, itemEntity.getType(), chunkPos);
    }

    /**
     * Cancel natural/spawner/egg finalize early when the chunk/team is already at cap.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || ChunkLimitService.canBypass(player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        EntityType<?> type = resolveSpawnableType(stack);
        if (type == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = player.chunkPosition();
        if (!MobLimitService.wouldExceedIfAdded(level, chunkPos, type, true)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        MobLimitService.denySpawnAttempt(player, type, chunkPos);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || ChunkLimitService.canBypass(player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        EntityType<?> type = resolveSpawnableType(stack);
        if (type == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        if (!MobLimitService.wouldExceedIfAdded(level, chunkPos, type, true)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        MobLimitService.denySpawnAttempt(player, type, chunkPos);
    }

    private static EntityType<?> resolveSpawnableType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof SpawnEggItem egg) {
            return egg.getType(stack);
        }
        // Mob buckets
        var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        return switch (itemId.getPath()) {
            case "axolotl_bucket" -> EntityType.AXOLOTL;
            case "cod_bucket" -> EntityType.COD;
            case "salmon_bucket" -> EntityType.SALMON;
            case "pufferfish_bucket" -> EntityType.PUFFERFISH;
            case "tropical_fish_bucket" -> EntityType.TROPICAL_FISH;
            case "tadpole_bucket" -> EntityType.TADPOLE;
            default -> null;
        };
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide() || event.isSpawnCancelled()) {
            return;
        }
        if (SunkenCityRespawnService.isRefilling() || CataclysmStructureRespawnService.isRefilling()) {
            return;
        }
        // Farm spawners never keep the mob — loot/blood/XP path cancels join. Cancelling here
        // skips that path and looks like "this egg gives nothing".
        if (SpawnerLootEvents.isFarmSpawnerFinalize(event)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || !MobLimitService.shouldEnforce(mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunkPos = mob.chunkPosition();
        if (!MobLimitService.wouldExceedIfAdded(level, chunkPos, mob.getType(), true)) {
            return;
        }
        event.setSpawnCancelled(true);
        event.setCanceled(true);
        // Spawn cancelled before join — egg/bucket usually not consumed yet; message only.
        ServerPlayer player = level.getNearestPlayer(mob, 8.0D) instanceof ServerPlayer nearest ? nearest : null;
        if (player != null && !ChunkLimitService.canBypass(player)) {
            MobLimitService.denySpawnAttempt(player, mob.getType(), chunkPos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (SunkenCityRespawnService.isRefilling() || CataclysmStructureRespawnService.isRefilling()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            return;
        }
        // Let SpawnerLootInterceptor deposit byproducts and cancel; do not discard first.
        if (entity instanceof Mob mob && SpawnerSpawnTracker.hasFarmSpawn(mob)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || !MobLimitService.shouldEnforce(entity)) {
            return;
        }

        ServerPlayer player = MobLimitService.findResponsiblePlayer(entity);
        if (player != null && ChunkLimitService.canBypass(player)) {
            return;
        }

        ChunkPos chunkPos = entity.chunkPosition();
        // Entity is already counted for join; loadedFromDisk entities are culled on chunk load.
        if (event.loadedFromDisk()) {
            level.getServer().execute(() -> {
                if (!entity.isRemoved() && MobLimitService.wouldExceed(level, chunkPos, entity)) {
                    MobLimitService.discardQuietly(entity);
                    MobLimitService.syncTeamIndex(level, chunkPos);
                } else {
                    MobLimitService.syncTeamIndex(level, chunkPos);
                }
            });
            return;
        }

        if (!MobLimitService.wouldExceedJoining(level, chunkPos, entity)) {
            MobLimitService.syncTeamIndex(level, chunkPos);
            return;
        }

        event.setCanceled(true);
        MobLimitService.discardQuietly(entity);
        if (player != null) {
            MobLimitService.denySpawn(player, entity, chunkPos);
        }
        level.getServer().execute(() -> MobLimitService.syncTeamIndex(level, chunkPos));
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyEntityLimits()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!MobLimitService.shouldEnforce(entity)) {
            return;
        }
        ChunkPos chunkPos = entity.chunkPosition();
        level.getServer().execute(() -> MobLimitService.syncTeamIndex(level, chunkPos));
    }
}
