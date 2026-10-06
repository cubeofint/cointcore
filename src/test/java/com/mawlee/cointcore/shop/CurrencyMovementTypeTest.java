package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
