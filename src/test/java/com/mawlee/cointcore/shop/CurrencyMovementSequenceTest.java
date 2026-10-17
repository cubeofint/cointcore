package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyMovementSequenceTest {
    @Test
    void nextIdIsMaxOfLocalAndAcceptedPlusOne() {
        assertEquals(1L, CurrencyMovementSequence.reconcile(0L, 0L).nextId());
        assertEquals(218L, CurrencyMovementSequence.reconcile(1L, 217L).nextId());
        assertEquals(300L, CurrencyMovementSequence.reconcile(300L, 217L).nextId());
        assertEquals(218L, CurrencyMovementSequence.reconcile(50L, 217L).acceptedUpTo() + 1L);
    }

    @Test
    void mergePrefersHighestCounters() {
        CurrencyMovementSequence.Snapshot snap = CurrencyMovementSequence.merge(1L, 40L, 0L, 217L);
        assertEquals(218L, snap.nextId());
        assertEquals(217L, snap.acceptedUpTo());
    }

    @Test
    void applyAcceptedRaisesNextIdAfterWipe() {
        CurrencyMovementSequence.Snapshot afterWipe = CurrencyMovementSequence.reconcile(1L, 0L);
        CurrencyMovementSequence.Snapshot recovered = CurrencyMovementSequence.applyAccepted(afterWipe, 217L);
        assertEquals(218L, recovered.nextId());
        assertEquals(217L, recovered.acceptedUpTo());
    }
}
