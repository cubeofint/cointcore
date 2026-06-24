package com.mawlee.cointcore.vanish;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.List;

public final class VanishServerStatus {
    private static final int SAMPLE_LIMIT = 12;

    private VanishServerStatus() {
    }

    public static ServerStatus.Players filter(MinecraftServer server, ServerStatus.Players players) {
        if (players == null || VanishManager.vanishedPlayers().isEmpty()) {
            return players;
        }

        PlayerList playerList = server.getPlayerList();
        List<GameProfile> visibleSample = new ArrayList<>(SAMPLE_LIMIT);
        int visibleOnline = 0;

        for (ServerPlayer player : playerList.getPlayers()) {
            if (VanishManager.isVanished(player)) {
                continue;
            }

            visibleOnline++;
            if (visibleSample.size() < SAMPLE_LIMIT) {
                visibleSample.add(player.getGameProfile());
            }
        }

        return new ServerStatus.Players(players.max(), visibleOnline, visibleSample);
    }

    public static void invalidate(MinecraftServer server) {
        if (server != null) {
            server.invalidateStatus();
        }
    }
}
