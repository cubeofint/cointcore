package com.mawlee.cointcore.mute;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class MuteEvents {
    private static final int CHECK_INTERVAL = 20;

    private MuteEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!MuteManager.isMuted(player)) {
            return;
        }

        event.setCanceled(true);
        MuteService.notifyIfMuted(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (MuteManager.cleanupIfExpired(player.server, player.getUUID())) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.MUTE_LIFTED));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MuteManager.cleanupIfExpired(player.server, player.getUUID());
        }
    }
}
