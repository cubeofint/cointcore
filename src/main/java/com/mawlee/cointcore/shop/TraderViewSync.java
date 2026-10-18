package com.mawlee.cointcore.shop;

import java.util.List;
import java.util.Objects;

/**
 * Equality for shop/manage menu snapshots. Used to skip redundant server→client
 * sync and to decide whether the client must rebuild widgets (row identity).
 * {@link net.minecraft.world.item.ItemStack} is not compared by reference.
 */
public final class TraderViewSync {
    private TraderViewSync() {
    }

    public static boolean sameShopView(
            long balanceA,
            String ownerA,
            List<PlayerTraderListing> listingsA,
            long balanceB,
            String ownerB,
            List<PlayerTraderListing> listingsB
    ) {
        return balanceA == balanceB
                && Objects.equals(nullToEmpty(ownerA), nullToEmpty(ownerB))
                && sameListings(listingsA, listingsB);
    }

    public static boolean sameListings(List<PlayerTraderListing> a, List<PlayerTraderListing> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int index = 0; index < a.size(); index++) {
            if (!sameListing(a.get(index), b.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static boolean sameListing(PlayerTraderListing a, PlayerTraderListing b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        TraderOffer left = a.offer();
        TraderOffer right = b.offer();
        return a.listingId().equals(b.listingId())
                && a.dealsLeft() == b.dealsLeft()
                && a.createdAt() == b.createdAt()
                && a.expiresAt() == b.expiresAt()
                && a.marketUnitPrice() == b.marketUnitPrice()
                && Objects.equals(a.sellerId(), b.sellerId())
                && Objects.equals(a.sellerName(), b.sellerName())
                && left.count() == right.count()
                && left.buyPrice() == right.buyPrice()
                && left.sellPrice() == right.sellPrice()
                && left.buyFee() == right.buyFee()
                && left.buyTotal() == right.buyTotal()
                && left.sellFee() == right.sellFee()
                && left.sellNet() == right.sellNet();
    }

    public static boolean sameManageView(
            String ownerA,
            long revenueA,
            int returnsA,
            List<PlayerShopOfferSnapshot> offersA,
            List<MarketPriceHint> hintsA,
            String ownerB,
            long revenueB,
            int returnsB,
            List<PlayerShopOfferSnapshot> offersB,
            List<MarketPriceHint> hintsB
    ) {
        return Objects.equals(nullToEmpty(ownerA), nullToEmpty(ownerB))
                && revenueA == revenueB
                && returnsA == returnsB
                && sameOffers(offersA, offersB)
                && Objects.equals(hintsA == null ? List.of() : hintsA, hintsB == null ? List.of() : hintsB);
    }

    public static boolean sameOffers(List<PlayerShopOfferSnapshot> a, List<PlayerShopOfferSnapshot> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int index = 0; index < a.size(); index++) {
            if (!sameOffer(a.get(index), b.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static boolean sameOffer(PlayerShopOfferSnapshot a, PlayerShopOfferSnapshot b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.id(), b.id())
                && a.count() == b.count()
                && a.buyPrice() == b.buyPrice()
                && a.sellPrice() == b.sellPrice()
                && a.stockItems() == b.stockItems();
    }

    /**
     * Widget layout for the current page depends only on listing ids (and count).
     * Stock/price changes must not rebuild {@code EditBox} widgets.
     */
    public static boolean sameListingIds(List<PlayerTraderListing> a, List<PlayerTraderListing> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int index = 0; index < a.size(); index++) {
            PlayerTraderListing left = a.get(index);
            PlayerTraderListing right = b.get(index);
            if (left == null || right == null || !left.listingId().equals(right.listingId())) {
                return false;
            }
        }
        return true;
    }

    public static boolean sameOfferIds(List<PlayerShopOfferSnapshot> a, List<PlayerShopOfferSnapshot> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int index = 0; index < a.size(); index++) {
            PlayerShopOfferSnapshot left = a.get(index);
            PlayerShopOfferSnapshot right = b.get(index);
            if (left == null || right == null || !Objects.equals(left.id(), right.id())) {
                return false;
            }
        }
        return true;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
