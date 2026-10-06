package com.mawlee.cointcore.invsee;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InvSeeSessions {
    private static final Map<UUID, InvSeeSession> ACTIVE = new ConcurrentHashMap<>();

    private InvSeeSessions() {
    }

    public static InvSeeSession begin(ServerPlayer viewer, InvSeeTarget target) {
        close(viewer);
        InvSeeSession session = new InvSeeSession(viewer, target);
        ACTIVE.put(viewer.getUUID(), session);
        return session;
    }

    public static InvSeeSession require(ServerPlayer viewer) {
        InvSeeSession session = ACTIVE.get(viewer.getUUID());
        if (session == null) {
            throw new IllegalStateException("No active InvSee session for " + viewer.getGameProfile().getName());
        }
        return session;
    }

    public static void close(ServerPlayer viewer) {
        InvSeeSession session = ACTIVE.remove(viewer.getUUID());
        if (session != null) {
            session.close();
        }
    }

    public static void close(InvSeeSession session) {
        ACTIVE.remove(session.viewerId());
        session.close();
    }
}
