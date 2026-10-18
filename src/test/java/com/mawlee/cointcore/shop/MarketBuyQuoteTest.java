package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBuyQuoteTest {
    @Test
    void unitsAndDebitFollowDealSize() {
        assertEquals(30L, MarketBuyQuote.units(3L, 10));
        Commission.Result fee = Commission.ofDeal(100L, 3L, 2.5d);
        assertEquals(fee.total(), MarketBuyQuote.debit(3L, 100L, 2.5d));
        assertEquals(307L, fee.total());
        assertEquals(0L, MarketBuyQuote.debit(0L, 100L, 2.5d));
    }

    @Test
    void maxDealsIsStockAndWallet() {
        assertEquals(2L, MarketBuyQuote.maxDeals(30L, 10, 100L, 250L, 2.5d));
        assertEquals(3L, MarketBuyQuote.maxDeals(30L, 10, 100L, 10_000L, 2.5d));
        assertEquals(0L, MarketBuyQuote.maxDeals(5L, 10, 100L, 10_000L, 2.5d));
    }

    @Test
    void canFillIsAllOrNothing() {
        assertTrue(MarketBuyQuote.canFill(2L, 5L, 200L, 200L));
        assertFalse(MarketBuyQuote.canFill(3L, 2L, 200L, 10_000L));
        assertFalse(MarketBuyQuote.canFill(2L, 5L, 201L, 200L));
    }
}
