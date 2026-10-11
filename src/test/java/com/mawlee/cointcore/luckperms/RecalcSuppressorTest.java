package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecalcSuppressorTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void tearDown() {
        RecalcSuppressor.reset();
    }

    @Test
    void ownWriteRecalcIsIgnoredDuringWindow() {
        RecalcSuppressor.markOwnWrite(PLAYER, 1_000L, 2_000L);
        assertTrue(RecalcSuppressor.shouldIgnoreRecalc(PLAYER, 1_500L));
        assertTrue(RecalcSuppressor.shouldIgnoreRecalc(PLAYER, 2_999L));
        assertFalse(RecalcSuppressor.shouldIgnoreRecalc(PLAYER, 3_000L));
    }

    @Test
    void inFlightReconcileIgnoresNewRecalc() {
        assertTrue(RecalcSuppressor.tryBegin(PLAYER));
        assertFalse(RecalcSuppressor.tryBegin(PLAYER));
        assertTrue(RecalcSuppressor.shouldIgnoreRecalc(PLAYER, 10L));
        RecalcSuppressor.end(PLAYER);
        assertFalse(RecalcSuppressor.shouldIgnoreRecalc(PLAYER, 10L));
        assertTrue(RecalcSuppressor.tryBegin(PLAYER));
    }
}
