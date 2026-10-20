package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ChatChannelNetwork {
    private ChatChannelNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToClient(
                ChatChannelMessagePayload.TYPE,
                ChatChannelMessagePayload.STREAM_CODEC,
                ChatChannelMessagePayload::handleClient
        );
        registrar.playToClient(
                ChatViewPayload.TYPE,
                ChatViewPayload.STREAM_CODEC,
                ChatViewPayload::handleClient
        );
    }
}
