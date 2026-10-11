package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixSiteAckTest {
    @Test
    void mutationFailureIsFailedRetryableNotRejectedUnavailable() {
        PrefixSiteAck ack = PrefixSiteAck.from(PrefixSelectionRules.Decision.failed());
        assertEquals("failed", ack.status());
        assertEquals(PrefixSelectionRules.REASON_RETRYABLE, ack.reason());
        assertNull(ack.selected());
        assertFalse(ack.persist());
    }

    @Test
    void missingGroupStaysRejectedUnavailable() {
        PrefixSiteAck ack = PrefixSiteAck.from(PrefixSelectionRules.Decision.unavailable("admin"));
        assertEquals("rejected", ack.status());
        assertEquals(PrefixSelectionRules.REASON_UNAVAILABLE, ack.reason());
        assertTrue(ack.persist());
    }

    @Test
    void appliedAndAutoArePersisted() {
        PrefixSiteAck applied = PrefixSiteAck.from(PrefixSelectionRules.Decision.applied("titan"));
        assertEquals("applied", applied.status());
        assertEquals("titan", applied.selected());
        assertTrue(applied.persist());

        PrefixSiteAck auto = PrefixSiteAck.from(PrefixSelectionRules.Decision.auto());
        assertEquals("applied", auto.status());
        assertEquals(PrefixSelectionRules.REASON_AUTO, auto.reason());
        assertNull(auto.selected());
        assertTrue(auto.persist());
    }
}
