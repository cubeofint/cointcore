package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ChunkLimitEvents {
    private ChunkLimitEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || ChunkLimitService.canBypass(player)) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        if (!ChunkLimitService.wouldExceedBlockLimit(level, chunkPos, placedState.getBlock())) {
            return;
        }

        event.setCanceled(true);
        ChunkLimitService.denyBlockPlacement(player, placedState, chunkPos);
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
        if (!ChunkLimitConfig.hasEntityLimit(itemEntity.getType())) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = itemEntity.chunkPosition();
        if (!ChunkLimitService.wouldExceedEntityLimit(level, chunkPos, itemEntity.getType())) {
            return;
        }

        event.setCanceled(true);
        ChunkLimitService.denyItemToss(player, itemEntity.getType(), chunkPos);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || event.loadedFromDisk() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasEntityLimit(entity.getType())) {
            return;
        }

        ChunkPos chunkPos = entity.chunkPosition();
        if (!ChunkLimitService.wouldExceedEntityLimit(level, chunkPos, entity.getType())) {
            return;
        }

        ServerPlayer player = ChunkLimitService.findResponsiblePlayer(entity);
        if (player != null && ChunkLimitService.canBypass(player)) {
            return;
        }

        event.setCanceled(true);
        if (player != null && ChunkLimitService.isPlayerInitiatedEntity(entity)) {
            ChunkLimitService.denyEntitySpawn(player, entity, chunkPos);
        }
    }
}
