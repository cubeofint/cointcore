package com.mawlee.cointcore.server;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class WorldCleanupEvents {
    private WorldCleanupEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        WorldCleanupService.resetRuntimeState();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WorldCleanupService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WorldCleanupService.resetRuntimeState();
    }
}
