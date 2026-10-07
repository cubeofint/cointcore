package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeCursorGuardTest {
    @Test
    void restoresOnSameTick() {
        assertTrue(InvSeeCursorGuard.shouldRestore(true, 10, 10));
    }

    @Test
    void restoresOnNextTick() {
        assertTrue(InvSeeCursorGuard.shouldRestore(true, 10, 11));
    }

    @Test
    void ignoresStaleCapture() {
        assertFalse(InvSeeCursorGuard.shouldRestore(true, 10, 12));
        assertFalse(InvSeeCursorGuard.shouldRestore(false, 10, 10));
    }
}
