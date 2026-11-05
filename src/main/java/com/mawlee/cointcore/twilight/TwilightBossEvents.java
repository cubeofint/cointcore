package com.mawlee.cointcore.twilight;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.TwilightBossRespawnConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/** Tracks Twilight Forest bosses as they appear and die, and puts dead bosses' spawners back once per start. */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class TwilightBossEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int pendingTick = -1;

    private TwilightBossEvents() {
    }

    private static boolean active() {
        return ModList.get().isLoaded("twilightforest");
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level) || !active()) {
            return;
        }
        Entity entity = event.getEntity();
        TwilightBosses.homeOf(entity).ifPresent(home -> {
            if (TwilightBossSavedData.get(level.getServer())
                    .bossJoined(home.pos().dimension(), home.pos().pos(), home.spawner(), entity.getUUID())) {
                LOGGER.info("Twilight boss arena recorded: {} at {} in {}", home.spawner(), home.pos().pos().toShortString(),
                        home.pos().dimension().location());
            }
        });
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level) || !active()) {
            return;
        }
        Entity entity = event.getEntity();
        Entity.RemovalReason reason = entity.getRemovalReason();        // Killed or discarded; chunk unloads and dimension changes keep the boss alive.
        if (reason != null && reason.shouldDestroy() && TwilightBosses.homeOf(entity).isPresent()) {
            TwilightBossSavedData.get(level.getServer()).bossGone(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (active() && TwilightBossRespawnConfig.isEnabled()) {
            pendingTick = event.getServer().getTickCount() + TwilightBossRespawnConfig.getDelayTicks();
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
            TwilightBosses.respawnAll(server);
        } catch (Throwable throwable) {
            LOGGER.error("Twilight boss respawn failed", throwable);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        pendingTick = -1;
    }
}
