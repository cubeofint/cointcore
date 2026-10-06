package com.mawlee.cointcore.shop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GluonWallet {
    private GluonWallet() {
    }

    public static long get(MinecraftServer server, UUID playerId) {
        return GluonWalletSavedData.get(server).get(playerId);
    }

    public static long get(ServerPlayer player) {
        return get(player.server, player.getUUID());
    }

    public static long set(MinecraftServer server, UUID playerId, long amount) {
        return GluonWalletSavedData.get(server).set(playerId, amount);
    }

    public static long add(MinecraftServer server, UUID playerId, long amount) {
        return GluonWalletSavedData.get(server).add(playerId, amount);
    }

    public static boolean trySubtract(MinecraftServer server, UUID playerId, long amount) {
        return GluonWalletSavedData.get(server).trySubtract(playerId, amount);
    }
}
