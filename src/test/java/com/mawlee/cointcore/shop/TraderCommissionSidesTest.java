package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraderCommissionSidesTest {
    @Test
    void buyerPaysPricePlusFee() {
        Commission.Result buy = Commission.of(100L, 2.5d);
        assertEquals(3L, buy.fee());
        assertEquals(103L, buy.total());
    }

    @Test
    void sellerReceivesPriceMinusFee() {
        Commission.Result sell = Commission.of(80L, 2.5d);
        assertEquals(2L, sell.fee());
        assertEquals(78L, 80L - sell.fee());
    }
}
