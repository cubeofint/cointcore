package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class InvSeeNetwork {
    private InvSeeNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CointCore.MOD_ID).optional();
        registrar.playToClient(
                InvSeeChromePayload.TYPE,
                InvSeeChromePayload.STREAM_CODEC,
                InvSeeChromePayload::handleClient
        );
        registrar.playToClient(
                InvSeeInfoPayload.TYPE,
                InvSeeInfoPayload.STREAM_CODEC,
                InvSeeInfoPayload::handleClient
        );
        registrar.playToClient(
                InvSeeCuriosLayoutPayload.TYPE,
                InvSeeCuriosLayoutPayload.STREAM_CODEC,
                InvSeeCuriosLayoutPayload::handleClient
        );
        registrar.playToServer(
                InvSeeOpenNestedPayload.TYPE,
                InvSeeOpenNestedPayload.STREAM_CODEC,
                InvSeeOpenNestedPayload::handleServer
        );
    }
}
