package com.mawlee.cointcore.shop;

/**
 * Pure escrow / listing arithmetic for the server-wide player market.
 * Callers saturate overflow; prices and deal counts are never taken from the client as-is.
 */
public final class GlobalMarketMath {
    public static final int DEFAULT_MAX_LISTINGS = 20;
    public static final int DEFAULT_LIFETIME_DAYS = 7;
    public static final int MAX_DEALS = 9_999;
    public static final long DAY_MS = 86_400_000L;

    private GlobalMarketMath() {
    }

    public static int clampDeals(int deals) {
        if (deals < 1) {
            return 0;
        }
        return Math.min(deals, MAX_DEALS);
    }

    public static int clampPrice(int price) {
        if (price < 1) {
            return 0;
        }
        return Math.min(price, (int) Math.min(Integer.MAX_VALUE, PlayerShopValidation.MAX_PRICE));
    }

    public static long listingFee(long configured) {
        if (configured <= 0L) {
            return 0L;
        }
        return Math.min(configured, PlayerShopValidation.MAX_PRICE);
    }

    public static long expiresAt(long createdAt, int lifetimeDays) {
        int days = Math.max(1, lifetimeDays);
        long created = Math.max(0L, createdAt);
        if (days > 0 && created > Long.MAX_VALUE - (long) days * DAY_MS) {
            return Long.MAX_VALUE;
        }
        return created + (long) days * DAY_MS;
    }

    public static boolean expired(long now, long expiresAt) {
        return expiresAt > 0L && now >= expiresAt;
    }

    public static int dealsFromStock(int stockItems, int countPerDeal) {
        return TraderDealMath.itemUnits(stockItems, countPerDeal);
    }

    public static int leftoverItems(int stockItems, int countPerDeal) {
        int count = Math.max(1, countPerDeal);
        if (stockItems <= 0) {
            return 0;
        }
        return stockItems - dealsFromStock(stockItems, count) * count;
    }

    public static int itemsForDeals(int countPerDeal, int deals) {
        int count = Math.max(1, countPerDeal);
        int safeDeals = clampDeals(deals);
        if (safeDeals < 1) {
            return 0;
        }
        if (count > Integer.MAX_VALUE / safeDeals) {
            return Integer.MAX_VALUE;
        }
        return count * safeDeals;
    }

    public static long buyerDebit(long pricePerDeal, long feePerDeal, int deals) {
        long unit = saturatingAdd(Math.max(0L, pricePerDeal), Math.max(0L, feePerDeal));
        return TraderDealMath.cost(unit, clampDeals(deals));
    }

    public static long sellerCredit(long pricePerDeal, int deals) {
        return TraderDealMath.cost(Math.max(0L, pricePerDeal), clampDeals(deals));
    }

    public static int maxAffordableDeals(long unitTotal, long balance, int available) {
        return TraderDealMath.affordableUnits(unitTotal, balance, clampDeals(available));
    }

    public static boolean canCreateListing(int currentListings, int maxListings, int deals, int price) {
        if (currentListings < 0 || currentListings >= Math.max(1, maxListings)) {
            return false;
        }
        return clampDeals(deals) >= 1 && clampPrice(price) >= 1;
    }

    public static boolean canCancel(String actorId, String sellerId, boolean admin) {
        if (actorId == null || actorId.isBlank() || sellerId == null || sellerId.isBlank()) {
            return false;
        }
        return actorId.equals(sellerId) || admin;
    }

    public static boolean canBuyOwn(String buyerId, String sellerId) {
        return buyerId != null && sellerId != null && buyerId.equals(sellerId);
    }

    static long saturatingAdd(long left, long right) {
        long sum = left + right;
        if (((left ^ sum) & (right ^ sum)) < 0L) {
            return Long.MAX_VALUE;
        }
        return sum;
    }
}
