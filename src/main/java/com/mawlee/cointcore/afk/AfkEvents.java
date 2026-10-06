package com.mawlee.cointcore.afk;

import com.mawlee.cointcore.CointCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class AfkEvents {
    private AfkEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        AfkService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkService.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkService.onPlayerLeave(player);
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        AfkService.touch(event.getPlayer(), AfkService.ACTIVITY_CHAT, AfkActivityStrength.STRONG);
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            AfkService.touch(player, AfkService.ACTIVITY_COMMAND, AfkActivityStrength.STRONG);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AfkService.clearRuntimeState();
    }
}
