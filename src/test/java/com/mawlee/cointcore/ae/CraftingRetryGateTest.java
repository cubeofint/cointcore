package com.mawlee.cointcore.ae;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CraftingRetryGateTest {
    @Test void firstRequestsAreImmediateAndFailedRequestsResumeAtTheBoundary() {
        var gate = new CraftingRetryGate<String>();
        var level = new Object();
        assertFalse(gate.shouldDelay(0, "stone", 64, level, 100, 20));
        gate.failed(0, "stone", 64, level, 100);
        assertTrue(gate.shouldDelay(0, new String("stone"), 64, level, 100, 20));
        assertTrue(gate.shouldDelay(0, "stone", 64, level, 119, 20));
        assertFalse(gate.shouldDelay(0, "stone", 64, level, 120, 20));
        assertFalse(gate.shouldDelay(0, "stone", 64, level, 121, 20));
    }

    @Test void changingTheItemAmountDimensionOrSlotDoesNotDelayANewRequest() {
        var level = new Object();
        var gate = new CraftingRetryGate<String>();
        gate.failed(0, "stone", 64, level, 100);
        assertFalse(gate.shouldDelay(1, "stone", 64, level, 101, 20));
        assertFalse(gate.shouldDelay(0, "dirt", 64, level, 101, 20));
        gate.failed(0, "stone", 64, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 32, level, 101, 20));
        gate.failed(0, "stone", 64, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 64, new Object(), 101, 20));
    }

    @Test void configChangesAndClockRewindsDoNotTrapRequests() {
        var level = new Object();
        var gate = new CraftingRetryGate<String>();
        gate.failed(0, "stone", 1, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 1, level, 101, 1));
        gate.failed(0, "stone", 1, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 1, level, 101, 0));
        gate.failed(0, "stone", 1, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 1, level, 90, 20));
        gate.failed(0, "stone", 1, level, 100);
        assertFalse(gate.shouldDelay(0, "stone", 1, level, 110, 5));
    }
}
