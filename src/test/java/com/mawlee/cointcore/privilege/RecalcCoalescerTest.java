package com.mawlee.cointcore.privilege;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecalcCoalescerTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void queuesAtMostOnePendingCheckPerPlayer() {
        RecalcCoalescer coalescer = new RecalcCoalescer(10);
        assertTrue(coalescer.tryQueue(PLAYER));
        assertTrue(coalescer.isPending(PLAYER));
        assertFalse(coalescer.tryQueue(PLAYER));
        assertTrue(coalescer.finishAndShouldRun(PLAYER, 0));
        assertFalse(coalescer.isPending(PLAYER));
        assertTrue(coalescer.tryQueue(PLAYER));
    }

    @Test
    void minIntervalSkipsImmediateRerun() {
        RecalcCoalescer coalescer = new RecalcCoalescer(10);
        assertTrue(coalescer.tryQueue(PLAYER));
        assertTrue(coalescer.finishAndShouldRun(PLAYER, 5));
        assertTrue(coalescer.tryQueue(PLAYER));
        assertFalse(coalescer.finishAndShouldRun(PLAYER, 14));
        assertTrue(coalescer.tryQueue(PLAYER));
        assertTrue(coalescer.finishAndShouldRun(PLAYER, 15));
    }
}
