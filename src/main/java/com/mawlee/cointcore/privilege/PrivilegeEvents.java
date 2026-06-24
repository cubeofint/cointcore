package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.nightvision.NightVisionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class PrivilegeEvents {
    private static final int NIGHT_VISION_CHECK_INTERVAL = 20;

    private PrivilegeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % NIGHT_VISION_CHECK_INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PrivilegeEnforcement.enforceNightVision(player);
            NightVisionService.reapplyIfEnabled(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> NightVisionService.onPlayerRespawn(player));
        }
    }
}
