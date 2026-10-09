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

    public static boolean tryTransfer(MinecraftServer server, UUID fromId, UUID toId, long amount) {
        return GluonWalletSavedData.get(server).tryTransfer(fromId, toId, amount);
    }

    /**
     * Customer purchase at a player shop: debit {@code customerDebit} (price + fee),
     * credit {@code ownerCredit} (listing price). Owner may be offline.
     */
    public static boolean tryPlayerShopBuy(
            MinecraftServer server,
            UUID customerId,
            UUID ownerId,
            long customerDebit,
            long ownerCredit
    ) {
        return GluonWalletSavedData.get(server).tryPlayerShopBuy(customerId, ownerId, customerDebit, ownerCredit);
    }

    /**
     * Owner buying from a customer: debit {@code ownerDebit} (listing price),
     * credit {@code customerCredit} (price − fee). Owner may be offline.
     */
    public static boolean tryPlayerShopSell(
            MinecraftServer server,
            UUID ownerId,
            UUID customerId,
            long ownerDebit,
            long customerCredit
    ) {
        return GluonWalletSavedData.get(server).tryPlayerShopSell(ownerId, customerId, ownerDebit, customerCredit);
    }

    public static boolean reversePlayerShopSell(
            MinecraftServer server,
            UUID ownerId,
            UUID customerId,
            long ownerDebit,
            long customerCredit
    ) {
        return GluonWalletSavedData.get(server).reversePlayerShopSell(ownerId, customerId, ownerDebit, customerCredit);
    }
}
