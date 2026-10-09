package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public final class PlayerTraderDeals {
    private PlayerTraderDeals() {
    }

    public static void buy(ServerPlayer player, PlayerTraderMenu menu, PlayerTraderBlockEntity shop, int offerIndex, boolean stack) {
        PlayerShopOffer stored = validOffer(shop, offerIndex);
        if (stored == null || !matchesShownListing(menu, offerIndex, stored)) {
            fail(player, menu, shop, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        if (player.getUUID().equals(shop.ownerId())) {
            fail(player, menu, shop, TraderFeedbackKind.OWN_SHOP, 0, "", 0L, 0L, CointCoreMessages.PLAYER_SHOP_OWN);
            return;
        }
        TraderOffer offer = stored.toTraderOffer(TraderOffersConfig.playerShopCommissionPercent());
        if (!offer.canBuy()) {
            fail(player, menu, shop, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        ItemStack sample = stored.template();
        synchronized (shop) {
            int requested = stack
                    ? TraderDealMath.unitsPerStack(stored.count(), sample.getMaxStackSize())
                    : 1;
            int byStock = TraderDealMath.itemUnits(ShopContainers.countMatching(shop.stock(), sample), stored.count());
            if (byStock < 1) {
                fail(player, menu, shop, TraderFeedbackKind.OUT_OF_STOCK, 0, itemName(offer), 0L, 0L, CointCoreMessages.PLAYER_SHOP_OUT_OF_STOCK);
                return;
            }
            int bySpace = TraderDealMath.itemUnits(TraderInventory.spaceFor(player.getInventory(), sample), stored.count());
            if (bySpace < 1) {
                fail(player, menu, shop, TraderFeedbackKind.INVENTORY_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_INVENTORY_FULL);
                return;
            }
            long balance = GluonWallet.get(player);
            int byMoney = TraderDealMath.affordableUnits(offer.buyTotal(), balance, requested);
            if (byMoney < 1) {
                fail(
                        player,
                        menu,
                        shop,
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
            int units = TraderDealMath.resolveUnits(requested, Math.min(byStock, bySpace), byMoney);
            long totalCost = TraderDealMath.cost(offer.buyTotal(), units);
            long totalPrice = TraderDealMath.cost(offer.buyPrice(), units);
            long totalFee = TraderDealMath.cost(offer.buyFee(), units);
            ItemStack goods = stored.sample();
            goods.setCount(stored.count() * units);
            UUID ownerId = shop.ownerId();
            if (ownerId == null
                    || !PlayerShopValidation.settleBuyLegal(
                    new PlayerShopValidation.UUIDPair(player.getUUID().toString(), ownerId.toString()),
                    totalPrice,
                    totalCost
            )) {
                fail(player, menu, shop, TraderFeedbackKind.OWN_SHOP, 0, "", 0L, 0L, CointCoreMessages.PLAYER_SHOP_OWN);
                return;
            }
            if (!TraderInventory.canFullyFit(player.getInventory(), goods)) {
                fail(player, menu, shop, TraderFeedbackKind.INVENTORY_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_INVENTORY_FULL);
                return;
            }
            if (!shop.takeStockForSale(sample, stored.count() * units)) {
                fail(player, menu, shop, TraderFeedbackKind.OUT_OF_STOCK, 0, itemName(offer), 0L, 0L, CointCoreMessages.PLAYER_SHOP_OUT_OF_STOCK);
                return;
            }
            if (!GluonWallet.tryPlayerShopBuy(player.server, player.getUUID(), ownerId, totalCost, totalPrice)) {
                if (!shop.storePurchase(goods.copy()) && shop.getLevel() != null) {
                    Block.popResource(shop.getLevel(), shop.getBlockPos(), goods);
                }
                fail(
                        player,
                        menu,
                        shop,
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
            TraderInventory.giveOrFail(player.getInventory(), goods);
            shop.recordSaleRevenue(totalPrice);
            long buyerAfter = GluonWallet.get(player.server, player.getUUID());
            long ownerAfter = GluonWallet.get(player.server, ownerId);
            CurrencyMovementService.record(
                    player.server,
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    ownerId,
                    shop.ownerName(),
                    totalPrice,
                    CurrencyMovementType.PLAYER_SHOP_BUY,
                    stored.id(),
                    List.of(
                            new CurrencyMovement.Delta(player.getUUID(), -totalCost, buyerAfter),
                            new CurrencyMovement.Delta(ownerId, totalPrice, ownerAfter)
                    ),
                    null
            );
            menu.refresh(GluonWallet.get(player), shop.ownerName(), PlayerTraderMenus.listingsOf(shop));
            int totalItems = stored.count() * units;
            succeed(
                    player,
                    menu,
                    shop,
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
    }

    public static void sell(ServerPlayer player, PlayerTraderMenu menu, PlayerTraderBlockEntity shop, int offerIndex, boolean stack) {
        PlayerShopOffer stored = validOffer(shop, offerIndex);
        if (stored == null || !matchesShownListing(menu, offerIndex, stored)) {
            fail(player, menu, shop, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        if (player.getUUID().equals(shop.ownerId())) {
            fail(player, menu, shop, TraderFeedbackKind.OWN_SHOP, 0, "", 0L, 0L, CointCoreMessages.PLAYER_SHOP_OWN);
            return;
        }
        TraderOffer offer = stored.toTraderOffer(TraderOffersConfig.playerShopCommissionPercent());
        if (!offer.canSell()) {
            fail(player, menu, shop, TraderFeedbackKind.OFFER_UNAVAILABLE, 0, "", 0L, 0L, CointCoreMessages.TRADER_OFFER_UNAVAILABLE);
            return;
        }
        ItemStack sample = stored.template();
        synchronized (shop) {
            int requested = stack
                    ? TraderDealMath.unitsPerStack(stored.count(), sample.getMaxStackSize())
                    : 1;
            int matching = TraderInventory.countMatching(player.getInventory(), sample);
            int byItems = TraderDealMath.itemUnits(matching, stored.count());
            if (byItems < 1) {
                fail(player, menu, shop, TraderFeedbackKind.NOT_ENOUGH_ITEMS, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS);
                return;
            }
            int bySpace = TraderDealMath.itemUnits(ShopContainers.spaceFor(shop.stock(), sample), stored.count());
            if (bySpace < 1) {
                fail(player, menu, shop, TraderFeedbackKind.STOCK_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.PLAYER_SHOP_STOCK_FULL);
                return;
            }
            int units = Math.min(requested, Math.min(byItems, bySpace));
            int removeCount = stored.count() * units;
            ItemStack goods = stored.sample();
            goods.setCount(removeCount);
            UUID ownerId = shop.ownerId();
            long totalNet = TraderDealMath.cost(offer.sellNet(), units);
            long totalPrice = TraderDealMath.cost(offer.sellPrice(), units);
            long totalFee = TraderDealMath.cost(offer.sellFee(), units);
            if (ownerId == null
                    || !PlayerShopValidation.settleSellLegal(
                    new PlayerShopValidation.UUIDPair(player.getUUID().toString(), ownerId.toString()),
                    totalPrice,
                    totalNet
            )) {
                fail(player, menu, shop, TraderFeedbackKind.OWN_SHOP, 0, "", 0L, 0L, CointCoreMessages.PLAYER_SHOP_OWN);
                return;
            }
            if (!ShopContainers.canFullyFit(shop.stock(), goods)) {
                fail(player, menu, shop, TraderFeedbackKind.STOCK_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.PLAYER_SHOP_STOCK_FULL);
                return;
            }
            if (!GluonWallet.tryPlayerShopSell(player.server, ownerId, player.getUUID(), totalPrice, totalNet)) {
                fail(
                        player,
                        menu,
                        shop,
                        TraderFeedbackKind.OWNER_BROKE,
                        0,
                        itemName(offer),
                        totalPrice,
                        totalFee,
                        CointCoreMessages.PLAYER_SHOP_OWNER_BROKE
                );
                return;
            }
            if (!TraderInventory.removeMatching(player.getInventory(), sample, removeCount)) {
                GluonWallet.reversePlayerShopSell(player.server, ownerId, player.getUUID(), totalPrice, totalNet);
                fail(player, menu, shop, TraderFeedbackKind.NOT_ENOUGH_ITEMS, 0, itemName(offer), 0L, 0L, CointCoreMessages.TRADER_NOT_ENOUGH_ITEMS);
                return;
            }
            if (!shop.storePurchase(goods)) {
                TraderInventory.giveOrFail(player.getInventory(), goods);
                GluonWallet.reversePlayerShopSell(player.server, ownerId, player.getUUID(), totalPrice, totalNet);
                fail(player, menu, shop, TraderFeedbackKind.STOCK_FULL, 0, itemName(offer), 0L, 0L, CointCoreMessages.PLAYER_SHOP_STOCK_FULL);
                return;
            }
            long buyerAfter = GluonWallet.get(player.server, player.getUUID());
            long ownerAfter = GluonWallet.get(player.server, ownerId);
            CurrencyMovementService.record(
                    player.server,
                    ownerId,
                    shop.ownerName(),
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    totalNet,
                    CurrencyMovementType.PLAYER_SHOP_SELL,
                    stored.id(),
                    List.of(
                            new CurrencyMovement.Delta(ownerId, -totalPrice, ownerAfter),
                            new CurrencyMovement.Delta(player.getUUID(), totalNet, buyerAfter)
                    ),
                    null
            );
            menu.refresh(GluonWallet.get(player), shop.ownerName(), PlayerTraderMenus.listingsOf(shop));
            succeed(
                    player,
                    menu,
                    shop,
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
    }

    /**
     * The client picks an offer by row index. Reject the deal when the owner changed that
     * row (item, lot size or prices) after this customer's catalog was last sent, so a
     * price edit can never be charged against a stale row.
     */
    private static boolean matchesShownListing(PlayerTraderMenu menu, int index, PlayerShopOffer current) {
        List<PlayerTraderListing> shown = menu.listings();
        if (index < 0 || index >= shown.size()) {
            return false;
        }
        TraderOffer seen = shown.get(index).offer();
        TraderOffer now = current.toTraderOffer(TraderOffersConfig.playerShopCommissionPercent());
        return seen.id().equals(now.id())
                && seen.buyPrice() == now.buyPrice()
                && seen.sellPrice() == now.sellPrice()
                && seen.buyTotal() == now.buyTotal()
                && seen.sellNet() == now.sellNet()
                && seen.count() == now.count()
                && ItemStack.isSameItemSameComponents(seen.display(), now.display());
    }

    private static PlayerShopOffer validOffer(PlayerTraderBlockEntity shop, int index) {
        List<PlayerShopOffer> valid = shop.validOffers();
        if (index < 0 || index >= valid.size()) {
            return null;
        }
        return valid.get(index);
    }

    private static String itemName(TraderOffer offer) {
        return offer.display().getHoverName().getString();
    }

    private static void fail(
            ServerPlayer player,
            PlayerTraderMenu menu,
            PlayerTraderBlockEntity shop,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        notify(player, menu, shop, kind, count, itemName, gluons, fee, chatKey, chatArgs);
    }

    private static void succeed(
            ServerPlayer player,
            PlayerTraderMenu menu,
            PlayerTraderBlockEntity shop,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        notify(player, menu, shop, kind, count, itemName, gluons, fee, chatKey, chatArgs);
    }

    private static void notify(
            ServerPlayer player,
            PlayerTraderMenu menu,
            PlayerTraderBlockEntity shop,
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
        List<PlayerTraderListing> listings = PlayerTraderMenus.listingsOf(shop);
        long balance = GluonWallet.get(player);
        menu.refresh(balance, shop.ownerName(), listings);
        PacketDistributor.sendToPlayer(
                player,
                new PlayerTraderCatalogPayload(menu.containerId, balance, shop.ownerName(), listings)
        );
        if (chatArgs.length == 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey));
        } else {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey, chatArgs));
        }
    }
}
