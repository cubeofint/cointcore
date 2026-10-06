package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClaimFlagEditNetwork {
    private ClaimFlagEditNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToClient(
                ClaimFlagEditSyncPayload.TYPE,
                ClaimFlagEditSyncPayload.STREAM_CODEC,
                ClaimFlagEditSyncPayload::handleClient
        );
    }
}
