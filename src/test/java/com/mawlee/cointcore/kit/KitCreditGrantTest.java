package com.mawlee.cointcore.kit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitCreditGrantTest {
    private static final UUID PLAYER = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void appliedOnServerThread() {
        KitCreditGrant.Decision decision = KitCreditGrant.decide(PLAYER, "vip", 2, true, true);
        assertEquals(KitCreditGrant.APPLIED, decision.status());
        assertTrue(decision.applyNow());
        assertFalse(decision.schedule());
    }

    @Test
    void scheduledOffServerThread() {
        KitCreditGrant.Decision decision = KitCreditGrant.decide(PLAYER, "vip", 2, true, false);
        assertEquals(KitCreditGrant.SCHEDULED, decision.status());
        assertTrue(decision.schedule());
        assertFalse(decision.applyNow());
    }

    @Test
    void noServerWhenServerMissing() {
        KitCreditGrant.Decision decision = KitCreditGrant.decide(PLAYER, "vip", 2, false, false);
        assertEquals(KitCreditGrant.NO_SERVER, decision.status());
        assertFalse(decision.applyNow());
        assertFalse(decision.schedule());
    }

    @Test
    void invalidWhenArgsBad() {
        assertEquals(KitCreditGrant.INVALID, KitCreditGrant.decide(null, "vip", 1, true, true).status());
        assertEquals(KitCreditGrant.INVALID, KitCreditGrant.decide(PLAYER, "  ", 1, true, true).status());
        assertEquals(KitCreditGrant.INVALID, KitCreditGrant.decide(PLAYER, "vip", 0, true, true).status());
        assertEquals(KitCreditGrant.INVALID, KitCreditGrant.decide(PLAYER, "vip", -3, true, true).status());
        assertFalse(KitCreditGrant.validGrant(PLAYER, null, 1));
    }
}
