package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TraderNetwork {
    private TraderNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToServer(
                TraderTradePayload.TYPE,
                TraderTradePayload.STREAM_CODEC,
                TraderTradePayload::handleServer
        );
        registrar.playToClient(
                TraderFeedbackPayload.TYPE,
                TraderFeedbackPayload.STREAM_CODEC,
                TraderFeedbackPayload::handleClient
        );
    }
}
