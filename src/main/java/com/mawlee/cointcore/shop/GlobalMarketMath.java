package com.mawlee.cointcore.shop;

/**
 * Pure escrow / listing arithmetic for the server-wide player market.
 * Deal count per lot is unbounded; stock is a {@code long}.
 */
public final class GlobalMarketMath {
    public static final int DEFAULT_MAX_LISTINGS = 20;
    public static final int DEFAULT_LIFETIME_DAYS = 7;
    public static final long DEFAULT_MAX_STOCK_PER_LOT = 1_000_000L;
    public static final long DAY_MS = 86_400_000L;

    private GlobalMarketMath() {
    }

    public static long clampMaxStockPerLot(long configured) {
        return configured < 0L ? 0L : configured;
    }

    public static long clampDeals(long deals) {
        return deals < 1L ? 0L : deals;
    }

    public static int clampDeals(int deals) {
        long value = clampDeals((long) deals);
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
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

    public static long dealsFromStock(long stockItems, int countPerDeal) {
        long count = Math.max(1, countPerDeal);
        if (stockItems <= 0L) {
            return 0L;
        }
        return stockItems / count;
    }

    public static int dealsFromStock(int stockItems, int countPerDeal) {
        long deals = dealsFromStock((long) stockItems, countPerDeal);
        return deals > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) deals;
    }

    public static long leftoverItems(long stockItems, int countPerDeal) {
        long count = Math.max(1, countPerDeal);
        if (stockItems <= 0L) {
            return 0L;
        }
        return stockItems - dealsFromStock(stockItems, (int) count) * count;
    }

    public static int leftoverItems(int stockItems, int countPerDeal) {
        long leftover = leftoverItems((long) stockItems, countPerDeal);
        return leftover > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) leftover;
    }

    public static long itemsForDeals(int countPerDeal, long deals) {
        long count = Math.max(1, countPerDeal);
        long safeDeals = clampDeals(deals);
        if (safeDeals < 1L) {
            return 0L;
        }
        if (count > 0L && safeDeals > Long.MAX_VALUE / count) {
            return Long.MAX_VALUE;
        }
        return count * safeDeals;
    }

    public static int itemsForDeals(int countPerDeal, int deals) {
        long items = itemsForDeals(countPerDeal, (long) deals);
        return items > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) items;
    }

    public static long buyerDebit(long pricePerDeal, long feePerDeal, long deals) {
        long unit = saturatingAdd(Math.max(0L, pricePerDeal), Math.max(0L, feePerDeal));
        return saturatingMul(unit, clampDeals(deals));
    }

    public static long sellerCredit(long pricePerDeal, long deals) {
        return saturatingMul(Math.max(0L, pricePerDeal), clampDeals(deals));
    }

    public static long maxAffordableDeals(long unitTotal, long balance, long available) {
        if (unitTotal <= 0L || balance < unitTotal || available <= 0L) {
            return 0L;
        }
        return Math.min(available, balance / unitTotal);
    }

    public static boolean canCreateListing(int currentListings, int maxListings, long deals, int price) {
        if (currentListings < 0 || currentListings >= Math.max(1, maxListings)) {
            return false;
        }
        return clampDeals(deals) >= 1L && clampPrice(price) >= 1;
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

    static long saturatingMul(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}
