package com.mawlee.cointcore.server;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.spark.SparkMetricsService;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ServerAutomationEvents {
    private ServerAutomationEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerRestartService.tick(server);

        if (server.getTickCount() % 20 != 0) {
            return;
        }

        ScheduledRestartService.tick(server);
        PeriodicMessageService.tick(server);
        SparkProfilerService.tick(server);
        SparkMetricsService.tick(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ScheduledRestartService.resetRuntimeState();
        PeriodicMessageService.resetRuntimeState();
        SparkProfilerService.resetRuntimeState();
        SparkMetricsService.resetRuntimeState();
        ServerRestartService.cancelPendingRestart();
    }
}
