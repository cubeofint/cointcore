package com.mawlee.cointcore.shop;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Minimum unit price per item id ({@code minecraft:diamond}) or item tag ({@code #c:gems/diamond}) for global market
 * lots. When an item matches several rules the highest minimum wins. Minecraft-free, unit-tested.
 */
public record MarketMinPrices(Map<String, Long> byItem, Map<String, Long> byTag) {
    public static final MarketMinPrices NONE = new MarketMinPrices(Map.of(), Map.of());

    /** Highest minimum unit price for this item, or 0 when no rule matches. */
    public long minUnitPrice(String itemId, Stream<String> tagIds) {
        long min = itemId == null ? 0L : byItem.getOrDefault(itemId, 0L);
        if (!byTag.isEmpty()) {
            min = Math.max(min, tagIds.mapToLong(tag -> byTag.getOrDefault(tag, 0L)).max().orElse(0L));
        }
        return Math.max(0L, min);
    }

    /** True when a lot of {@code countPerDeal} items for {@code pricePerDeal} costs less than the minimum per item. */
    public static boolean belowMinimum(long pricePerDeal, int countPerDeal, long minUnitPrice) {
        if (minUnitPrice <= 0L) {
            return false;
        }
        long count = Math.max(1, countPerDeal);
        return minUnitPrice > Long.MAX_VALUE / count || pricePerDeal < minUnitPrice * count;
    }
}
