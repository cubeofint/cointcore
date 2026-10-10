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
        String modId
) {
    public PlayerTraderListing(TraderOffer offer, int stockItems) {
        this(offer.id(), offer, Math.max(0, stockItems / Math.max(1, offer.count())), "", "", 0L, "");
    }

    public int stockItems() {
        return dealsLeft * Math.max(1, offer.count());
    }

    public boolean inStock() {
        return dealsLeft >= 1;
    }
}
