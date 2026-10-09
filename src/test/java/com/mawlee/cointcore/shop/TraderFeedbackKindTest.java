package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderFeedbackKindTest {
    @Test
    void errorKindsAreErrorsAndDealsAreNot() {
        assertTrue(TraderFeedbackKind.NOT_ENOUGH_GLUONS.error());
        assertTrue(TraderFeedbackKind.NOT_ENOUGH_ITEMS.error());
        assertTrue(TraderFeedbackKind.INVENTORY_FULL.error());
        assertTrue(TraderFeedbackKind.OFFER_UNAVAILABLE.error());
        assertTrue(TraderFeedbackKind.OUT_OF_STOCK.error());
        assertTrue(TraderFeedbackKind.STOCK_FULL.error());
        assertTrue(TraderFeedbackKind.OWNER_BROKE.error());
        assertTrue(TraderFeedbackKind.OWN_SHOP.error());
        assertFalse(TraderFeedbackKind.BOUGHT.error());
        assertFalse(TraderFeedbackKind.SOLD.error());
    }

    @Test
    void fromOrdinalFallsBackWhenOutOfRange() {
        assertEquals(TraderFeedbackKind.BOUGHT, TraderFeedbackKind.fromOrdinal(TraderFeedbackKind.BOUGHT.ordinal()));
        assertEquals(TraderFeedbackKind.OFFER_UNAVAILABLE, TraderFeedbackKind.fromOrdinal(-1));
        assertEquals(TraderFeedbackKind.OFFER_UNAVAILABLE, TraderFeedbackKind.fromOrdinal(99));
    }
}
