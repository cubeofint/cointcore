package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class TraderTrades {
    private TraderTrades() {
    }

    public static void buy(ServerPlayer player, TraderMenu menu, int offerIndex, boolean stack) {
        TraderOffer offer = offer(menu.offers(), offerIndex);
        if (offer == null || !offer.canBuy()) {
            fail(player, menu, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        ItemStack sample = offer.createGoods();
        int requested = stack
                ? TraderDealMath.unitsPerStack(offer.count(), sample.getMaxStackSize())
                : 1;
        int bySpace = TraderDealMath.itemUnits(TraderInventory.spaceFor(player.getInventory(), sample), offer.count());
        if (bySpace < 1) {
            fail(player, menu, TraderFeedbackKind.INVENTORY_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_INVENTORY_FULL);
            return;
        }
        long balance = GluonWallet.get(player);
        int byMoney = TraderDealMath.affordableUnits(offer.buyTotal(), balance, requested);
        if (byMoney < 1) {
            fail(
                    player,
                    menu,
                    TraderFeedbackKind.NOT_ENOUGH_GLUONS,
                    0,
                    itemName(offer),
                    offer.buyTotal(),
                    offer.buyFee(),
                    CointCoreMessages.TRADER_NOT_ENOUGH,
                    offer.buyTotal()
            );
            return;
        }
        int units = TraderDealMath.resolveUnits(requested, bySpace, byMoney);
        long totalCost = TraderDealMath.cost(offer.buyTotal(), units);
        long totalFee = TraderDealMath.cost(offer.buyFee(), units);
        ItemStack goods = offer.createGoods();
        goods.setCount(offer.count() * units);
        if (!TraderInventory.canFullyFit(player.getInventory(), goods)) {
            fail(player, menu, TraderFeedbackKind.INVENTORY_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_INVENTORY_FULL);
            return;
        }
        if (!GluonWallet.trySubtract(player.server, player.getUUID(), totalCost)) {
            fail(
                    player,
                    menu,
                    TraderFeedbackKind.NOT_ENOUGH_GLUONS,
                    0,
                    itemName(offer),
                    totalCost,
                    totalFee,
                    CointCoreMessages.TRADER_NOT_ENOUGH,
                    totalCost
            );
            return;
        }
        if (!TraderInventory.canFullyFit(player.getInventory(), goods)) {
            GluonWallet.add(player.server, player.getUUID(), totalCost);
            fail(player, menu, TraderFeedbackKind.INVENTORY_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_INVENTORY_FULL);
            return;
        }
        TraderInventory.giveOrFail(player.getInventory(), goods);
        CurrencyMovementService.record(
                player.server,
                player.getUUID(),
                player.getGameProfile().getName(),
                null,
                "trader",
                totalCost,
                CurrencyMovementType.TRADER_BUY,
                offer.id()
        );
        menu.refreshBalance(GluonWallet.get(player));
        int totalItems = offer.count() * units;
        succeed(
                player,
                menu,
                TraderFeedbackKind.BOUGHT,
                totalItems,
                itemName(offer),
                totalCost,
                totalFee,
                CointCoreMessages.TRADER_BOUGHT,
                totalItems,
                itemName(offer),
                totalCost,
                totalFee
        );
    }

    public static void sell(ServerPlayer player, TraderMenu menu, int offerIndex, boolean stack) {
        TraderOffer offer = offer(menu.offers(), offerIndex);
        if (offer == null || !offer.canSell()) {
            fail(player, menu, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        ItemStack sample = offer.createGoods();
        int requested = stack
                ? TraderDealMath.unitsPerStack(offer.count(), sample.getMaxStackSize())
                : 1;
        int matching = TraderInventory.countMatching(player.getInventory(), sample);
        int byItems = TraderDealMath.itemUnits(matching, offer.count());
        if (byItems < 1) {
            fail(player, menu, TraderFeedbackKind.NOT_ENOUGH_ITEMS, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS);
            return;
        }
        int units = Math.min(requested, byItems);
        int removeCount = offer.count() * units;
        if (!TraderInventory.removeMatching(player.getInventory(), sample, removeCount)) {
            fail(player, menu, TraderFeedbackKind.NOT_ENOUGH_ITEMS, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS);
            return;
        }
        long totalNet = TraderDealMath.cost(offer.sellNet(), units);
        long totalFee = TraderDealMath.cost(offer.sellFee(), units);
        GluonWallet.add(player.server, player.getUUID(), totalNet);
        CurrencyMovementService.record(
                player.server,
                null,
                "trader",
                player.getUUID(),
                player.getGameProfile().getName(),
                totalNet,
                CurrencyMovementType.TRADER_SELL,
                offer.id()
        );
        menu.refreshBalance(GluonWallet.get(player));
        succeed(
                player,
                menu,
                TraderFeedbackKind.SOLD,
                removeCount,
                itemName(offer),
                totalNet,
                totalFee,
                CointCoreMessages.TRADER_SOLD,
                removeCount,
                itemName(offer),
                totalNet,
                totalFee
        );
    }

    private static String itemName(TraderOffer offer) {
        return offer.display().getHoverName().getString();
    }

    private static void fail(
            ServerPlayer player,
            TraderMenu menu,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        notify(player, menu, kind, count, itemName, gluons, fee, chatKey, chatArgs);
    }

    private static void succeed(
            ServerPlayer player,
            TraderMenu menu,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        notify(player, menu, kind, count, itemName, gluons, fee, chatKey, chatArgs);
    }

    private static void notify(
            ServerPlayer player,
            TraderMenu menu,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new TraderFeedbackPayload(menu.containerId, kind, count, itemName, gluons, fee)
        );
        if (chatArgs.length == 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey));
        } else {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey, chatArgs));
        }
    }

    private static TraderOffer offer(List<TraderOffer> offers, int index) {
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }
}
