package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerShopValidationTest {
    @Test
    void offerNeedsItemAndAtLeastOnePrice() {
        assertFalse(PlayerShopValidation.offerValid(true, 1, 10L, 8L));
        assertFalse(PlayerShopValidation.offerValid(false, 0, 10L, 8L));
        assertFalse(PlayerShopValidation.offerValid(false, 1, 0L, 0L));
        assertTrue(PlayerShopValidation.offerValid(false, 1, 10L, 0L));
        assertTrue(PlayerShopValidation.offerValid(false, 8, 0L, 5L));
        assertTrue(PlayerShopValidation.offerValid(false, 1, 10L, 8L));
    }

    @Test
    void pricesAreClampedAndZeroDisablesASide() {
        assertEquals(0L, PlayerShopValidation.clampPrice(0L));
        assertEquals(0L, PlayerShopValidation.clampPrice(-3L));
        assertEquals(PlayerShopValidation.MAX_PRICE, PlayerShopValidation.clampPrice(Long.MAX_VALUE));
        assertEquals(1, PlayerShopValidation.clampCount(0));
        assertEquals(PlayerShopValidation.MAX_COUNT, PlayerShopValidation.clampCount(999));
    }

    @Test
    void buySettleMatchesPayStyleDeltasWithBurnedFee() {
        PlayerShopValidation.UUIDPair ids = new PlayerShopValidation.UUIDPair("buyer", "owner");
        assertTrue(PlayerShopValidation.settleBuyLegal(ids, 100L, 103L));
        assertFalse(PlayerShopValidation.settleBuyLegal(ids, 100L, 90L));
        assertFalse(PlayerShopValidation.settleBuyLegal(new PlayerShopValidation.UUIDPair("same", "same"), 100L, 103L));
    }

    @Test
    void sellSettleRequiresOwnerDebitAtLeastCustomerCredit() {
        PlayerShopValidation.UUIDPair ids = new PlayerShopValidation.UUIDPair("buyer", "owner");
        assertTrue(PlayerShopValidation.settleSellLegal(ids, 80L, 78L));
        assertFalse(PlayerShopValidation.settleSellLegal(ids, 80L, 0L));
        assertFalse(PlayerShopValidation.settleSellLegal(ids, 10L, 12L));
    }

    @Test
    void commissionSidesMatchSystemTrader() {
        Commission.Result buy = Commission.of(100L, 2.5d);
        assertEquals(3L, buy.fee());
        assertEquals(103L, buy.total());
        Commission.Result sell = Commission.of(80L, 2.5d);
        assertEquals(2L, sell.fee());
        assertEquals(78L, 80L - sell.fee());
        assertTrue(PlayerShopValidation.settleBuyLegal(
                new PlayerShopValidation.UUIDPair("a", "b"), 100L, buy.total()));
        assertTrue(PlayerShopValidation.settleSellLegal(
                new PlayerShopValidation.UUIDPair("a", "b"), 80L, 80L - sell.fee()));
    }

    @Test
    void stockUnitsUseTraderDealMath() {
        assertEquals(0, TraderDealMath.itemUnits(15, 16));
        assertEquals(2, TraderDealMath.itemUnits(32, 16));
        assertEquals(1, TraderDealMath.resolveUnits(64, 1, 10));
    }
}
