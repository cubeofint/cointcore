package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.CointCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/** Runs after {@code MuteEvents} (HIGHEST, cancels muted players); cancelled events are skipped. */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ChatChannelEvents {
    private ChatChannelEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ChatChannelService.onServerChat(event);
    }
}
