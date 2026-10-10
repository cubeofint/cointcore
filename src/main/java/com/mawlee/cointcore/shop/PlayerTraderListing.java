package com.mawlee.cointcore.shop;

/**
 * One global-market row shown in a vending machine.
 */
public record PlayerTraderListing(
        String listingId,
        TraderOffer offer,
        int dealsLeft,
        String sellerName,
        String sellerId,
        long createdAt,
        String modId,
        long expiresAt,
        long marketUnitPrice
) {
    public PlayerTraderListing {
        listingId = listingId == null ? "" : listingId;
        sellerName = sellerName == null ? "" : sellerName;
        sellerId = sellerId == null ? "" : sellerId;
        modId = modId == null ? "" : modId;
        dealsLeft = Math.max(0, dealsLeft);
        createdAt = Math.max(0L, createdAt);
        expiresAt = Math.max(0L, expiresAt);
        marketUnitPrice = Math.max(0L, marketUnitPrice);
    }

    public PlayerTraderListing(TraderOffer offer, int stockItems) {
        this(
                offer.id(),
                offer,
                Math.max(0, stockItems / Math.max(1, offer.count())),
                "",
                "",
                0L,
                "",
                0L,
                0L
        );
    }

    public int stockItems() {
        return dealsLeft * Math.max(1, offer.count());
    }

    public boolean inStock() {
        return dealsLeft >= 1;
    }

    public long unitPrice() {
        return GlobalMarketPriceMath.unitPrice(offer.buyPrice(), offer.count());
    }
}
