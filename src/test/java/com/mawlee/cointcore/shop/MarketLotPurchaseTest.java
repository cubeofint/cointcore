package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketLotPurchaseTest {
    private static final MarketLotPurchase.Snapshot LIVE = new MarketLotPurchase.Snapshot(
            true,
            true,
            false,
            80L,
            5L
    );

    @Test
    void missingLotFails() {
        assertEquals(
                MarketLotPurchase.Reason.MISSING,
                MarketLotPurchase.validate(null, 80L, 1L).reason()
        );
        assertEquals(
                MarketLotPurchase.Reason.MISSING,
                MarketLotPurchase.validate(MarketLotPurchase.Snapshot.missing(), 80L, 1L).reason()
        );
    }

    @Test
    void inactiveLotFails() {
        MarketLotPurchase.Snapshot inactive = new MarketLotPurchase.Snapshot(true, false, false, 80L, 5L);
        assertEquals(MarketLotPurchase.Reason.INACTIVE, MarketLotPurchase.validate(inactive, 80L, 1L).reason());
    }

    @Test
    void ownShopFails() {
        MarketLotPurchase.Snapshot own = new MarketLotPurchase.Snapshot(true, true, true, 80L, 5L);
        assertEquals(MarketLotPurchase.Reason.OWN_SHOP, MarketLotPurchase.validate(own, 80L, 1L).reason());
    }

    @Test
    void priceIncreaseFailsAndSameOrLowerPasses() {
        assertEquals(MarketLotPurchase.Reason.PRICE_UP, MarketLotPurchase.validate(LIVE, 79L, 1L).reason());
        assertTrue(MarketLotPurchase.validate(LIVE, 80L, 1L).ok());
        assertTrue(MarketLotPurchase.validate(LIVE, 100L, 1L).ok());
        assertTrue(MarketLotPurchase.validate(LIVE, 0L, 1L).ok());
    }

    @Test
    void stockMustCoverRequestedDeals() {
        assertEquals(MarketLotPurchase.Reason.NO_STOCK, MarketLotPurchase.validate(LIVE, 80L, 6L).reason());
        assertEquals(MarketLotPurchase.Reason.BAD_QTY, MarketLotPurchase.validate(LIVE, 80L, 0L).reason());
        assertTrue(MarketLotPurchase.validate(LIVE, 80L, 5L).ok());
        assertFalse(MarketLotPurchase.validate(LIVE, 80L, 6L).ok());
    }
}
