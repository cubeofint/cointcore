package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * One-shot: per-machine sell offers + matching stock become global listings;
 * leftover stock and buy-from-player-only offers go to the owner's return box.
 */
public final class GlobalMarketMigration {
    private GlobalMarketMigration() {
    }

    public static synchronized void migrate(MinecraftServer server, PlayerTraderBlockEntity shop) {
        if (shop == null || shop.marketMigrated() || shop.ownerId() == null) {
            if (shop != null && !shop.marketMigrated()) {
                shop.markMarketMigrated();
            }
            return;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        long now = System.currentTimeMillis();
        long expires = GlobalMarketMath.expiresAt(now, TraderOffersConfig.playerShopListingLifetimeDays());
        for (PlayerShopOffer offer : shop.offers()) {
            if (!offer.isValid() || offer.buyPrice() < 1L || offer.template().isEmpty()) {
                continue;
            }
            ItemStack sample = offer.template();
            int stock = ShopContainers.countMatching(shop.stock(), sample);
            int deals = GlobalMarketMath.dealsFromStock(stock, offer.count());
            int used = GlobalMarketMath.itemsForDeals(offer.count(), deals);
            if (deals >= 1 && used >= 1) {
                ShopContainers.removeMatching(shop.stock(), sample, used);
                ItemStack escrow = sample.copy();
                escrow.setCount(offer.count());
                UUID seller = offer.sellerId() != null ? offer.sellerId() : shop.ownerId();
                String name = offer.sellerId() != null && !offer.sellerName().isBlank()
                        ? offer.sellerName()
                        : shop.ownerName();
                int price = offer.buyPrice() > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : (int) offer.buyPrice();
                data.put(new GlobalMarketListing(
                        parseUuid(offer.id()),
                        seller,
                        name,
                        escrow,
                        deals,
                        GlobalMarketMath.clampPrice(price),
                        now,
                        expires,
                        0
                ));
            }
        }
        for (int slot = 0; slot < shop.stock().getContainerSize(); slot++) {
            ItemStack leftover = shop.stock().getItem(slot);
            if (!leftover.isEmpty()) {
                data.addReturn(shop.ownerId(), leftover.copy());
                shop.stock().setItem(slot, ItemStack.EMPTY);
            }
        }
        shop.clearOffersAfterMigration();
        shop.markMarketMigrated();
    }

    private static UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (RuntimeException ignored) {
            return UUID.randomUUID();
        }
    }
}
