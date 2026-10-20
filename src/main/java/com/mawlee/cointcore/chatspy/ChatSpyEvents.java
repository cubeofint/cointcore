package com.mawlee.cointcore.chatspy;

import com.mawlee.cointcore.CointCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ChatSpyEvents {
    private ChatSpyEvents() {
    }

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        ChatSpyTracker.flush(event.getServer());
    }
}
