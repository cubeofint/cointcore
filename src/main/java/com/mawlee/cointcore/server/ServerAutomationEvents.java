package com.mawlee.cointcore.server;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.adastra.AdAstraVortexPass;
import com.mawlee.cointcore.cataclysm.CataclysmStructureRespawnService;
import com.mawlee.cointcore.cataclysm.SunkenCityRespawnService;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mawlee.cointcore.spark.SparkMetricsService;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ServerAutomationEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerAutomationEvents() {
    }

    /**
     * Refresh MSPT-derived gates before world/BE work so SFM/RS/FA hot paths share one read.
     */
    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        PerfTickCache.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        AdAstraVortexPass.flush();
        ServerRestartService.tick(server);
        // Own interval (default 200 ticks ≈ 10s). Must not sit behind the 1s gate below.
        SunkenCityRespawnService.tick(server);
        CataclysmStructureRespawnService.tick(server);

        if (server.getTickCount() % 20 != 0) {
            return;
        }

        ScheduledRestartService.tick(server);
        DimensionWipeService.tick(server);
        PeriodicMessageService.tick(server);
        SparkProfilerService.tick(server);
        SparkMetricsService.tick(server);
        com.mawlee.cointcore.watchdog.WatchdogRetention.applySparkProfiles(server);
        com.mawlee.cointcore.watchdog.WatchdogRetention.applyWatchdogReports(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        try {
            DimensionWipeService.executePendingWipe(event.getServer());
        } catch (Throwable exception) {
            LOGGER.error("Dimension wipe failed during server stop", exception);
        }
        ScheduledRestartService.resetRuntimeState();
        DimensionWipeService.resetRuntimeState();
        PeriodicMessageService.resetRuntimeState();
        SparkProfilerService.resetRuntimeState();
        SparkMetricsService.resetRuntimeState();
        com.mawlee.cointcore.watchdog.TickWatchdogService.instance().stop();
        SunkenCityRespawnService.resetRuntimeState();
        CataclysmStructureRespawnService.resetRuntimeState();
        ServerRestartService.cancelPendingRestart();
        PerfTickCache.reset();
    }
}
