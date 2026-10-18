package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderViewSyncTest {
    @Test
    void manageHintsAndRevenueArePartOfTheView() {
        List<MarketPriceHint> hintsA = List.of(new MarketPriceHint("minecraft:stone", 4L));
        List<MarketPriceHint> hintsB = List.of(new MarketPriceHint("minecraft:stone", 5L));
        assertFalse(TraderViewSync.sameManageView("Ann", 1L, 0, null, hintsA, "Ann", 1L, 0, null, hintsB));
        assertTrue(TraderViewSync.sameManageView("Ann", 1L, 0, null, hintsA, "Ann", 1L, 0, null, hintsA));
        assertFalse(TraderViewSync.sameManageView("Ann", 1L, 0, null, hintsA, "Ann", 2L, 0, null, hintsA));
        assertFalse(TraderViewSync.sameManageView("Ann", 1L, 1, null, hintsA, "Ann", 1L, 0, null, hintsA));
    }

    @Test
    void shopOwnerAndBalanceMustMatch() {
        assertTrue(TraderViewSync.sameShopView(10L, "Ann", null, 10L, "Ann", null));
        assertFalse(TraderViewSync.sameShopView(10L, "Ann", null, 9L, "Ann", null));
        assertFalse(TraderViewSync.sameShopView(10L, "Ann", null, 10L, "Bob", null));
    }
}
