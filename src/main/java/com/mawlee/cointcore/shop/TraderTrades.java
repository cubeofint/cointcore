package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TraderTrades {
    private TraderTrades() {
    }

    public static void buy(ServerPlayer player, TraderMenu menu, int offerIndex) {
        TraderOffer offer = offer(menu.offers(), offerIndex);
        if (offer == null || !offer.canBuy()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_OFFER_UNAVAILABLE));
            return;
        }
        ItemStack goods = offer.createGoods();
        if (!TraderInventory.canFullyFit(player.getInventory(), goods)) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_INVENTORY_FULL));
            return;
        }
        if (!GluonWallet.trySubtract(player.server, player.getUUID(), offer.buyTotal())) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_NOT_ENOUGH, offer.buyTotal()));
            return;
        }
        if (!TraderInventory.canFullyFit(player.getInventory(), goods)) {
            GluonWallet.add(player.server, player.getUUID(), offer.buyTotal());
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_INVENTORY_FULL));
            return;
        }
        TraderInventory.giveOrFail(player.getInventory(), goods);
        CurrencyMovementService.record(
                player.server,
                player.getUUID(),
                player.getGameProfile().getName(),
                null,
                "trader",
                offer.buyTotal(),
                CurrencyMovementType.TRADER_BUY,
                offer.id()
        );
        menu.refreshBalance(GluonWallet.get(player));
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.TRADER_BOUGHT,
                offer.count(),
                offer.display().getHoverName().getString(),
                offer.buyTotal(),
                offer.buyFee()
        ));
    }

    public static void sell(ServerPlayer player, TraderMenu menu, int offerIndex) {
        TraderOffer offer = offer(menu.offers(), offerIndex);
        if (offer == null || !offer.canSell()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_OFFER_UNAVAILABLE));
            return;
        }
        ItemStack sample = offer.createGoods();
        if (TraderInventory.countMatching(player.getInventory(), sample) < offer.count()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS));
            return;
        }
        if (!TraderInventory.removeMatching(player.getInventory(), sample, offer.count())) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS));
            return;
        }
        GluonWallet.add(player.server, player.getUUID(), offer.sellNet());
        CurrencyMovementService.record(
                player.server,
                null,
                "trader",
                player.getUUID(),
                player.getGameProfile().getName(),
                offer.sellNet(),
                CurrencyMovementType.TRADER_SELL,
                offer.id()
        );
        menu.refreshBalance(GluonWallet.get(player));
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.TRADER_SOLD,
                offer.count(),
                offer.display().getHoverName().getString(),
                offer.sellNet(),
                offer.sellFee()
        ));
    }

    private static TraderOffer offer(List<TraderOffer> offers, int index) {
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }
}
