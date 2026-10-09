package com.mawlee.cointcore.shop;

/**
 * Pure checks for player-shop listings and settle amounts. No Minecraft types.
 */
public final class PlayerShopValidation {
    public static final int MAX_COUNT = 64;
    public static final long MAX_PRICE = 1_000_000_000L;

    private PlayerShopValidation() {
    }

    public static int clampCount(int count) {
        if (count < 1) {
            return 1;
        }
        return Math.min(count, MAX_COUNT);
    }

    public static long clampPrice(long price) {
        if (price <= 0L) {
            return 0L;
        }
        return Math.min(price, MAX_PRICE);
    }

    /**
     * A listing is valid when it has an item, count &gt;= 1, and at least one
     * direction with a whole-gluon price &gt;= 1.
     */
    public static boolean offerValid(boolean templateEmpty, int count, long buyPrice, long sellPrice) {
        if (templateEmpty || count < 1) {
            return false;
        }
        long buy = clampPrice(buyPrice);
        long sell = clampPrice(sellPrice);
        return buy >= 1L || sell >= 1L;
    }

    /**
     * Customer buy: customer is debited {@code buyTotal} (price + fee), owner is
     * credited {@code buyPrice}. Fee is burned (server commission).
     */
    public static boolean settleBuyLegal(UUIDPair ids, long buyPrice, long buyTotal) {
        if (!ids.distinct()) {
            return false;
        }
        return buyPrice >= 1L && buyTotal >= buyPrice;
    }

    /**
     * Customer sell: owner is debited {@code sellPrice}, customer is credited
     * {@code sellNet} (price − fee). Fee is burned.
     */
    public static boolean settleSellLegal(UUIDPair ids, long sellPrice, long sellNet) {
        if (!ids.distinct()) {
            return false;
        }
        return sellPrice >= 1L && sellNet >= 1L && sellPrice >= sellNet;
    }

    public record UUIDPair(String customerId, String ownerId) {
        public boolean distinct() {
            return customerId != null && ownerId != null && !customerId.equals(ownerId);
        }
    }
}
