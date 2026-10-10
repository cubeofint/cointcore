package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalMarketMathTest {
    @Test
    void escrowDealCountsSaturateAndLeaveRemainder() {
        assertEquals(3, GlobalMarketMath.dealsFromStock(10, 3));
        assertEquals(1, GlobalMarketMath.leftoverItems(10, 3));
        assertEquals(0, GlobalMarketMath.dealsFromStock(2, 3));
        assertEquals(64, GlobalMarketMath.itemsForDeals(16, 4));
        assertEquals(Integer.MAX_VALUE, GlobalMarketMath.itemsForDeals(2_000_000_000, 3));
        assertEquals(0, GlobalMarketMath.clampDeals(0));
        assertEquals(GlobalMarketMath.MAX_DEALS, GlobalMarketMath.clampDeals(Integer.MAX_VALUE));
    }

    @Test
    void buyMathUsesCommissionAndSaturates() {
        Commission.Result fee = Commission.of(100L, 2.5d);
        assertEquals(103L * 4L, GlobalMarketMath.buyerDebit(100L, fee.fee(), 4));
        assertEquals(400L, GlobalMarketMath.sellerCredit(100L, 4));
        assertEquals(2, GlobalMarketMath.maxAffordableDeals(103L, 250L, 10));
        assertEquals(0, GlobalMarketMath.maxAffordableDeals(103L, 100L, 10));
        assertEquals(Long.MAX_VALUE, GlobalMarketMath.buyerDebit(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, 3));
    }

    @Test
    void expiryUsesLifetimeDays() {
        long created = 1_000L;
        long expires = GlobalMarketMath.expiresAt(created, 7);
        assertEquals(created + 7L * GlobalMarketMath.DAY_MS, expires);
        assertFalse(GlobalMarketMath.expired(expires - 1L, expires));
        assertTrue(GlobalMarketMath.expired(expires, expires));
        assertEquals(Long.MAX_VALUE, GlobalMarketMath.expiresAt(Long.MAX_VALUE - 10L, 7));
    }

    @Test
    void listingPermissions() {
        assertTrue(GlobalMarketMath.canCreateListing(0, 20, 1, 5));
        assertFalse(GlobalMarketMath.canCreateListing(20, 20, 1, 5));
        assertFalse(GlobalMarketMath.canCreateListing(0, 20, 0, 5));
        assertFalse(GlobalMarketMath.canCreateListing(0, 20, 1, 0));
        assertTrue(GlobalMarketMath.canCancel("a", "a", false));
        assertFalse(GlobalMarketMath.canCancel("b", "a", false));
        assertTrue(GlobalMarketMath.canCancel("b", "a", true));
        assertTrue(GlobalMarketMath.canBuyOwn("a", "a"));
        assertFalse(GlobalMarketMath.canBuyOwn("a", "b"));
    }

    @Test
    void pricesAreWholeGluonsAtLeastOne() {
        assertEquals(0, GlobalMarketMath.clampPrice(0));
        assertEquals(0, GlobalMarketMath.clampPrice(-4));
        assertEquals(12, GlobalMarketMath.clampPrice(12));
        assertEquals(0L, GlobalMarketMath.listingFee(0L));
        assertEquals(7L, GlobalMarketMath.listingFee(7L));
    }
}
