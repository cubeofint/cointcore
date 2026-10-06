package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClaimFlagEditSync {
    private ClaimFlagEditSync() {
    }

    public static void init(MinecraftServer server) {
        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                server.execute(() -> sync(player));
            }
        });
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new ClaimFlagEditSyncPayload(ClaimFlagEditAccess.maskFor(player)));
    }
}
