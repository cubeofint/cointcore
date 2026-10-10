package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Median market price from a bounded window of completed global-market sales.
 * Item identity is a string key that ignores stack count.
 */
public final class GlobalMarketPriceMath {
    public static final int DEFAULT_WINDOW_DAYS = 14;
    public static final int DEFAULT_MAX_SALES = 200;

    private GlobalMarketPriceMath() {
    }

    public record Sale(String itemKey, long unitPrice, long timestamp) {
        public Sale {
            itemKey = itemKey == null ? "" : itemKey;
            unitPrice = Math.max(0L, unitPrice);
            timestamp = Math.max(0L, timestamp);
        }
    }

    public static String itemKey(String itemId, String components) {
        String id = itemId == null ? "" : itemId;
        String extra = components == null ? "" : components;
        return id + "|" + extra;
    }

    public static long unitPrice(long dealPrice, int count) {
        int n = Math.max(1, count);
        if (dealPrice <= 0L) {
            return 0L;
        }
        return Math.max(1L, dealPrice / n);
    }

    public static long dealPrice(long unitPrice, int count) {
        int n = Math.max(1, count);
        if (unitPrice <= 0L) {
            return 0L;
        }
        if (unitPrice > Long.MAX_VALUE / n) {
            return Long.MAX_VALUE;
        }
        return unitPrice * n;
    }

    public static long median(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> copy = new ArrayList<>(values.size());
        for (Long value : values) {
            if (value != null && value > 0L) {
                copy.add(value);
            }
        }
        if (copy.isEmpty()) {
            return 0L;
        }
        copy.sort(Long::compareTo);
        int n = copy.size();
        if ((n & 1) == 1) {
            return copy.get(n / 2);
        }
        long left = copy.get(n / 2 - 1);
        long right = copy.get(n / 2);
        return Math.round((left + right) / 2.0d);
    }

    public static List<Sale> prune(List<Sale> sales, long now, int windowDays, int maxSales) {
        if (sales == null || sales.isEmpty()) {
            return List.of();
        }
        int days = Math.max(1, windowDays);
        int cap = Math.max(1, maxSales);
        long cutoff = now > days * GlobalMarketMath.DAY_MS
                ? now - (long) days * GlobalMarketMath.DAY_MS
                : 0L;
        List<Sale> kept = new ArrayList<>();
        for (Sale sale : sales) {
            if (sale == null || sale.unitPrice() <= 0L || sale.itemKey().isBlank()) {
                continue;
            }
            if (sale.timestamp() >= cutoff) {
                kept.add(sale);
            }
        }
        kept.sort(Comparator.comparingLong(Sale::timestamp));
        if (kept.size() > cap) {
            kept = new ArrayList<>(kept.subList(kept.size() - cap, kept.size()));
        }
        return List.copyOf(kept);
    }

    public static List<Long> unitPrices(List<Sale> sales) {
        if (sales == null || sales.isEmpty()) {
            return List.of();
        }
        List<Long> prices = new ArrayList<>(sales.size());
        for (Sale sale : sales) {
            if (sale != null && sale.unitPrice() > 0L) {
                prices.add(sale.unitPrice());
            }
        }
        return prices;
    }

    /**
     * Median of recent unit prices, otherwise the cheapest current listing unit price.
     */
    public static long recommend(List<Long> recentUnitPrices, long lowestListingUnitPrice) {
        long median = median(recentUnitPrices);
        if (median > 0L) {
            return median;
        }
        return Math.max(0L, lowestListingUnitPrice);
    }
}
