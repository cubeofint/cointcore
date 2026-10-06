package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class InvSeeSessions {
    private static final Map<UUID, InvSeeSession> ACTIVE = new ConcurrentHashMap<>();

    private InvSeeSessions() {
    }

    public static InvSeeSession begin(ServerPlayer viewer, InvSeeTarget target, InvSeeSection section) {
        InvSeeSession previous = ACTIVE.remove(viewer.getUUID());
        if (previous != null) {
            if (previous.target() == target) {
                previous.abandonMenu();
            } else {
                previous.close();
            }
        }
        InvSeeSession session = new InvSeeSession(viewer, target, section);
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
        ACTIVE.remove(session.viewerId(), session);
        session.close();
    }

    public static void forEachViewer(InvSeeTarget target, Consumer<InvSeeSession> consumer) {
        for (InvSeeSession session : ACTIVE.values()) {
            if (session.target() == target) {
                consumer.accept(session);
            }
        }
    }

    public static void notifyTargetChanged(InvSeeTarget target) {
        MinecraftServer server = target.server();
        if (server == null) {
            return;
        }
        forEachViewer(target, session -> {
            ServerPlayer viewer = server.getPlayerList().getPlayer(session.viewerId());
            if (viewer == null) {
                return;
            }
            if (target.isFrozen()) {
                return;
            }
            if (target.isOffline()) {
                viewer.sendSystemMessage(CointCoreMessages.forPlayer(
                        viewer,
                        CointCoreMessages.INVSEE_TARGET_OFFLINE,
                        target.displayName()
                ));
            } else {
                viewer.sendSystemMessage(CointCoreMessages.forPlayer(
                        viewer,
                        CointCoreMessages.INVSEE_TARGET_ONLINE,
                        target.displayName()
                ));
            }
        });
    }
}
