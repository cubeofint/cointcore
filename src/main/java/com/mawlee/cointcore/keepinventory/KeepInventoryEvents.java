package com.mawlee.cointcore.keepinventory;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class KeepInventoryEvents {
    private KeepInventoryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepInventoryService.capture(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KeepInventoryService.hasPending(player)) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.server.execute(() -> KeepInventoryService.restore(player));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepInventoryService.clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        KeepInventoryService.clearRuntimeState();
    }
}
