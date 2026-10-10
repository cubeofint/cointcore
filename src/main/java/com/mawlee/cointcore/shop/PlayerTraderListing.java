package com.mawlee.cointcore.shop;

/**
 * One global-market row shown in a vending machine.
 */
public record PlayerTraderListing(
        String listingId,
        TraderOffer offer,
        long dealsLeft,
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
        dealsLeft = Math.max(0L, dealsLeft);
        createdAt = Math.max(0L, createdAt);
        expiresAt = Math.max(0L, expiresAt);
        marketUnitPrice = Math.max(0L, marketUnitPrice);
    }

    public PlayerTraderListing(TraderOffer offer, int stockItems) {
        this(
                offer.id(),
                offer,
                GlobalMarketMath.dealsFromStock(stockItems, offer.count()),
                "",
                "",
                0L,
                "",
                0L,
                0L
        );
    }

    public long stockItems() {
        return GlobalMarketMath.itemsForDeals(offer.count(), dealsLeft);
    }

    public boolean inStock() {
        return dealsLeft >= 1L;
    }

    public long unitPrice() {
        return GlobalMarketPriceMath.unitPrice(offer.buyPrice(), offer.count());
    }
}
