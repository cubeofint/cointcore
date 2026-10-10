package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Groups identical global-market lots (same item key including deal count) onto one row.
 * The cheapest listing is the buy target; extra sellers collapse to a count.
 */
public final class GlobalMarketRows {
    private GlobalMarketRows() {
    }

    public record Source(
            String listingId,
            String groupKey,
            int count,
            long price,
            int dealsLeft,
            String sellerName,
            long createdAt,
            long expiresAt,
            long marketUnitPrice,
            String modId
    ) {
        public Source {
            listingId = listingId == null ? "" : listingId;
            groupKey = groupKey == null ? "" : groupKey;
            count = Math.max(1, count);
            price = Math.max(0L, price);
            dealsLeft = Math.max(0, dealsLeft);
            sellerName = sellerName == null ? "" : sellerName;
            createdAt = Math.max(0L, createdAt);
            expiresAt = Math.max(0L, expiresAt);
            marketUnitPrice = Math.max(0L, marketUnitPrice);
            modId = modId == null ? "" : modId;
        }
    }

    public record Row(
            String listingId,
            int count,
            long price,
            int dealsLeft,
            int listingCount,
            List<String> sellers,
            long createdAt,
            long expiresAt,
            long marketUnitPrice,
            String modId
    ) {
        public Row {
            listingId = listingId == null ? "" : listingId;
            count = Math.max(1, count);
            price = Math.max(0L, price);
            dealsLeft = Math.max(0, dealsLeft);
            listingCount = Math.max(1, listingCount);
            sellers = sellers == null ? List.of() : List.copyOf(sellers);
            createdAt = Math.max(0L, createdAt);
            expiresAt = Math.max(0L, expiresAt);
            marketUnitPrice = Math.max(0L, marketUnitPrice);
            modId = modId == null ? "" : modId;
        }

        public boolean multiSeller() {
            return sellers.size() > 1;
        }

        public String cheapestSeller() {
            return sellers.isEmpty() ? "" : sellers.getFirst();
        }

        public long unitPrice() {
            return GlobalMarketPriceMath.unitPrice(price, count);
        }
    }

    public static List<Row> group(List<Source> listings, boolean sortByNew) {
        Map<String, List<Source>> buckets = new LinkedHashMap<>();
        if (listings != null) {
            for (Source source : listings) {
                if (source == null) {
                    continue;
                }
                buckets.computeIfAbsent(source.groupKey(), ignored -> new ArrayList<>()).add(source);
            }
        }
        List<Row> rows = new ArrayList<>(buckets.size());
        for (List<Source> bucket : buckets.values()) {
            bucket.sort(Comparator
                    .comparingLong(Source::price)
                    .thenComparingLong(Source::createdAt)
                    .thenComparing(Source::listingId));
            Source cheapest = bucket.getFirst();
            int deals = 0;
            Set<String> names = new LinkedHashSet<>();
            if (!cheapest.sellerName().isBlank()) {
                names.add(cheapest.sellerName());
            }
            for (Source source : bucket) {
                deals += source.dealsLeft();
                if (!source.sellerName().isBlank()) {
                    names.add(source.sellerName());
                }
            }
            rows.add(new Row(
                    cheapest.listingId(),
                    cheapest.count(),
                    cheapest.price(),
                    deals,
                    bucket.size(),
                    List.copyOf(names),
                    cheapest.createdAt(),
                    cheapest.expiresAt(),
                    cheapest.marketUnitPrice(),
                    cheapest.modId()
            ));
        }
        if (sortByNew) {
            rows.sort(Comparator.comparingLong(Row::createdAt).reversed());
        } else {
            rows.sort(Comparator.comparingLong(Row::price).thenComparing(Row::listingId));
        }
        return rows;
    }
}
