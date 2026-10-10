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
 * {@link Row#dealsLeft()} is stock at or below the shown unit price.
 */
public final class GlobalMarketRows {
    private GlobalMarketRows() {
    }

    public record Source(
            String listingId,
            String groupKey,
            int count,
            long price,
            long dealsLeft,
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
            dealsLeft = Math.max(0L, dealsLeft);
            sellerName = sellerName == null ? "" : sellerName;
            createdAt = Math.max(0L, createdAt);
            expiresAt = Math.max(0L, expiresAt);
            marketUnitPrice = Math.max(0L, marketUnitPrice);
            modId = modId == null ? "" : modId;
        }
    }

    public record SellerOffer(
            String listingId,
            String sellerName,
            long stockItems,
            long unitPrice,
            int countPerDeal,
            long pricePerDeal,
            long dealsLeft,
            long createdAt
    ) {
        public SellerOffer {
            listingId = listingId == null ? "" : listingId;
            sellerName = sellerName == null ? "" : sellerName;
            stockItems = Math.max(0L, stockItems);
            unitPrice = Math.max(0L, unitPrice);
            countPerDeal = Math.max(1, countPerDeal);
            pricePerDeal = Math.max(0L, pricePerDeal);
            dealsLeft = Math.max(0L, dealsLeft);
            createdAt = Math.max(0L, createdAt);
        }

        public static SellerOffer from(Source source) {
            Source safe = source == null
                    ? new Source("", "", 1, 0L, 0L, "", 0L, 0L, 0L, "")
                    : source;
            return new SellerOffer(
                    safe.listingId(),
                    safe.sellerName(),
                    GlobalMarketMath.itemsForDeals(safe.count(), safe.dealsLeft()),
                    GlobalMarketPriceMath.unitPrice(safe.price(), safe.count()),
                    safe.count(),
                    safe.price(),
                    safe.dealsLeft(),
                    safe.createdAt()
            );
        }
    }

    public record Row(
            String listingId,
            int count,
            long price,
            long dealsLeft,
            int listingCount,
            List<String> sellers,
            List<SellerOffer> offers,
            long createdAt,
            long expiresAt,
            long marketUnitPrice,
            String modId
    ) {
        public Row {
            listingId = listingId == null ? "" : listingId;
            count = Math.max(1, count);
            price = Math.max(0L, price);
            dealsLeft = Math.max(0L, dealsLeft);
            listingCount = Math.max(1, listingCount);
            sellers = sellers == null ? List.of() : List.copyOf(sellers);
            offers = offers == null ? List.of() : List.copyOf(offers);
            createdAt = Math.max(0L, createdAt);
            expiresAt = Math.max(0L, expiresAt);
            marketUnitPrice = Math.max(0L, marketUnitPrice);
            modId = modId == null ? "" : modId;
        }

        public boolean multiSeller() {
            return offers.size() > 1 || sellers.size() > 1;
        }

        public String cheapestSeller() {
            if (!offers.isEmpty()) {
                return offers.getFirst().sellerName();
            }
            return sellers.isEmpty() ? "" : sellers.getFirst();
        }

        public SellerOffer cheapestOffer() {
            return offers.isEmpty() ? null : offers.getFirst();
        }

        public SellerOffer offer(String listingId) {
            String id = listingId == null ? "" : listingId;
            for (SellerOffer offer : offers) {
                if (offer.listingId().equals(id)) {
                    return offer;
                }
            }
            return null;
        }

        public long unitPrice() {
            return GlobalMarketPriceMath.unitPrice(price, count);
        }

        public long stockItems() {
            return GlobalMarketMath.itemsForDeals(count, dealsLeft);
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
                    .comparingLong((Source source) -> GlobalMarketPriceMath.unitPrice(source.price(), source.count()))
                    .thenComparingLong(Source::createdAt)
                    .thenComparing(Source::listingId));
            Source cheapest = bucket.getFirst();
            long deals = 0L;
            Set<String> names = new LinkedHashSet<>();
            List<SellerOffer> offers = new ArrayList<>(bucket.size());
            if (!cheapest.sellerName().isBlank()) {
                names.add(cheapest.sellerName());
            }
            for (Source source : bucket) {
                if (source.price() <= cheapest.price()) {
                    deals = GlobalMarketMath.saturatingAdd(deals, source.dealsLeft());
                }
                if (!source.sellerName().isBlank()) {
                    names.add(source.sellerName());
                }
                offers.add(SellerOffer.from(source));
            }
            rows.add(new Row(
                    cheapest.listingId(),
                    cheapest.count(),
                    cheapest.price(),
                    deals,
                    bucket.size(),
                    List.copyOf(names),
                    List.copyOf(offers),
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
