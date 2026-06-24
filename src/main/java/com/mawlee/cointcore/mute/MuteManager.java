package com.mawlee.cointcore.mute;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class MuteManager {
    private MuteManager() {
    }

    public static boolean isMuted(ServerPlayer player) {
        return player != null && isMuted(player.server, player.getUUID());
    }

    public static boolean isMuted(MinecraftServer server, UUID playerId) {
        return playerId != null && MuteSavedData.get(server).get(playerId).isPresent();
    }

    public static Optional<MuteInfo> getMute(MinecraftServer server, UUID playerId) {
        return MuteSavedData.get(server).get(playerId);
    }

    public static void apply(MinecraftServer server, UUID playerId, MuteInfo info) {
        MuteSavedData.get(server).put(playerId, info);
    }

    public static void remove(MinecraftServer server, UUID playerId) {
        MuteSavedData.get(server).remove(playerId);
    }

    public static boolean cleanupIfExpired(MinecraftServer server, UUID playerId) {
        return MuteSavedData.get(server).cleanupIfExpired(playerId);
    }
}
