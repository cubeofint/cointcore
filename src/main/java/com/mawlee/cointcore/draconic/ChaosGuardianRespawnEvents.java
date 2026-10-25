package com.mawlee.cointcore.draconic;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.ChaosGuardianRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/** Runs {@link ChaosGuardianRespawn} once per server start, {@code delayTicks} after the server came up. */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ChaosGuardianRespawnEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int pendingTick = -1;

    private ChaosGuardianRespawnEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (ModList.get().isLoaded("draconicevolution") && ChaosGuardianRespawnConfig.isEnabled()) {
            pendingTick = event.getServer().getTickCount() + ChaosGuardianRespawnConfig.getDelayTicks();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (pendingTick < 0 || server.getTickCount() < pendingTick) {
            return;
        }
        pendingTick = -1;
        try {
            ChaosGuardianRespawn.run(server);
        } catch (Throwable throwable) {
            LOGGER.error("Chaos guardian respawn failed", throwable);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        pendingTick = -1;
    }
}
