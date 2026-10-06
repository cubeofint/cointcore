package com.mawlee.cointcore.seeinvisible;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SeeInvisibleNetwork {
    private SeeInvisibleNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToClient(
                SeeInvisibleSyncPayload.TYPE,
                SeeInvisibleSyncPayload.STREAM_CODEC,
                SeeInvisibleSyncPayload::handleClient
        );
    }
}
