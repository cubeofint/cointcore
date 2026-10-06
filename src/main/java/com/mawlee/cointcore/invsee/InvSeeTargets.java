package com.mawlee.cointcore.invsee;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InvSeeTargets {
    private static final Map<UUID, InvSeeTarget> LIVE = new ConcurrentHashMap<>();

    private InvSeeTargets() {
    }

    public static Optional<InvSeeTarget> acquire(MinecraftServer server, UUID playerId, String displayName) {
        InvSeeTarget target = LIVE.computeIfAbsent(playerId, id -> new InvSeeTarget(id, displayName, server));
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            if (target.getPlayer() != online) {
                target.attachOnline(online);
            }
            target.acquire();
            return Optional.of(target);
        }

        if (target.getPlayer() == null || !target.isOffline()) {
            if (!target.attachOffline(server, displayName)) {
                if (target.refs() <= 0) {
                    LIVE.remove(playerId, target);
                }
                return Optional.empty();
            }
        }
        target.acquire();
        return Optional.of(target);
    }

    public static Optional<InvSeeTarget> getIfPresent(UUID playerId) {
        return Optional.ofNullable(LIVE.get(playerId));
    }

    public static void release(InvSeeTarget target) {
        if (target.release() <= 0) {
            LIVE.remove(target.playerId(), target);
        }
    }

    public static void flushBeforeLoad(ServerPlayer player) {
        InvSeeTarget target = LIVE.get(player.getUUID());
        if (target != null && target.isOffline()) {
            target.flushBeforeJoin();
        }
    }

    public static void switchToOnline(ServerPlayer player) {
        InvSeeTarget target = LIVE.get(player.getUUID());
        if (target != null) {
            target.switchToOnline(player);
            InvSeeSessions.notifyTargetChanged(target);
        }
    }

    public static void freezeForLogout(ServerPlayer player) {
        InvSeeTarget target = LIVE.get(player.getUUID());
        if (target != null && !target.isOffline()) {
            target.freezeForLogout();
            InvSeeSessions.notifyTargetChanged(target);
        }
    }

    public static void switchToOfflineAfterSave(ServerPlayer player) {
        InvSeeTarget target = LIVE.get(player.getUUID());
        if (target != null && target.refs() > 0) {
            target.switchToOfflineAfterSave();
            InvSeeSessions.notifyTargetChanged(target);
        }
    }

    public static void tick(long nowMs) {
        for (InvSeeTarget target : LIVE.values()) {
            if (target.editLock().expireIfIdle(nowMs)) {
                InvSeeSessions.notifyTargetChanged(target);
            }
        }
    }

    public static void saveAllAndClear() {
        for (InvSeeTarget target : LIVE.values()) {
            target.saveIfDirty();
        }
        LIVE.clear();
    }
}
