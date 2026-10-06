package com.mawlee.cointcore.seeinvisible;

import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.vanish.VanishVisibility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SeeInvisibleService {
    private SeeInvisibleService() {
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
        boolean canSee = VanishVisibility.canSeeVanished(player);
        PacketDistributor.sendToPlayer(player, new SeeInvisibleSyncPayload(canSee));
    }
}
