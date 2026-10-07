package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyMovementTypeTest {
    @Test
    void roundTripsKnownIds() {
        for (CurrencyMovementType type : CurrencyMovementType.values()) {
            assertEquals(type, CurrencyMovementType.fromId(type.id()));
        }
    }

    @Test
    void unknownIdFallsBackToPay() {
        assertEquals(CurrencyMovementType.PAY, CurrencyMovementType.fromId("nope"));
        assertEquals(CurrencyMovementType.PAY, CurrencyMovementType.fromId(null));
    }

    @Test
    void onlyServerWalletLogsGoToTheSite() {
        assertTrue(CurrencyMovementType.PAY.isServerWalletLog());
        assertTrue(CurrencyMovementType.TRADER_BUY.isServerWalletLog());
        assertTrue(CurrencyMovementType.TRADER_SELL.isServerWalletLog());
        assertTrue(CurrencyMovementType.ADMIN_SET.isServerWalletLog());
        assertTrue(CurrencyMovementType.ADMIN_ADD.isServerWalletLog());
        assertFalse(CurrencyMovementType.SITE_TO_SERVER.isServerWalletLog());
        assertFalse(CurrencyMovementType.SERVER_TO_SITE.isServerWalletLog());
    }
}
