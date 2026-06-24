package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
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
}
