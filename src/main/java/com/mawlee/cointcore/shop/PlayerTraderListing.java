package com.mawlee.cointcore.shop;

/**
 * Customer-visible listing plus remaining matching stock (item count, not lots).
 */
public record PlayerTraderListing(TraderOffer offer, int stockItems) {
    public boolean inStock() {
        return stockItems >= offer.count();
    }
}
