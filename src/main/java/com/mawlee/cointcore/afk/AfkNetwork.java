package com.mawlee.cointcore.afk;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AfkNetwork {
    private AfkNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToServer(
                AfkGuiActivityPayload.TYPE,
                AfkGuiActivityPayload.STREAM_CODEC,
                AfkGuiActivityPayload::handleServer
        );
    }
}
