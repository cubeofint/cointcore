package com.mawlee.cointcore.enderio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemConduitNetworkThrottleTest {
    @BeforeEach
    void clear() {
        ItemConduitNetworkThrottle.clearAll();
    }

    @Test
    void runsOnFirstPass() {
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(1);
        assertEquals(
                ItemConduitNetworkThrottle.Decision.RUN,
                ItemConduitNetworkThrottle.shouldRun(state, 100L, 10, 40, 1L, true)
        );
    }

    @Test
    void backsOffAfterIdlePass() {
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(2);
        ItemConduitNetworkThrottle.onPassComplete(state, 100L, false, 10, 40, 7L);
        assertEquals(
                ItemConduitNetworkThrottle.Decision.SKIP_IDLE_BACKOFF,
                ItemConduitNetworkThrottle.shouldRun(state, 110L, 10, 40, 7L, true)
        );
        assertEquals(
                ItemConduitNetworkThrottle.Decision.RUN,
                ItemConduitNetworkThrottle.shouldRun(state, 140L, 10, 40, 8L, true)
        );
    }

    @Test
    void skipsUnchangedFingerprintAfterIdle() {
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(3);
        ItemConduitNetworkThrottle.onPassComplete(state, 50L, false, 10, 40, 99L);
        assertEquals(
                ItemConduitNetworkThrottle.Decision.SKIP_UNCHANGED,
                ItemConduitNetworkThrottle.shouldRun(state, 90L, 10, 40, 99L, true)
        );
    }

    @Test
    void activePassUsesShortInterval() {
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(4);
        ItemConduitNetworkThrottle.onPassComplete(state, 200L, true, 10, 40, 1L);
        assertEquals(
                ItemConduitNetworkThrottle.Decision.SKIP_INTERVAL,
                ItemConduitNetworkThrottle.shouldRun(state, 205L, 10, 40, 1L, true)
        );
        assertEquals(
                ItemConduitNetworkThrottle.Decision.RUN,
                ItemConduitNetworkThrottle.shouldRun(state, 210L, 10, 40, 2L, true)
        );
    }

    @Test
    void slotWindowCapsAndAdvances() {
        ItemConduitNetworkThrottle.State state = ItemConduitNetworkThrottle.stateFor(5);
        assertEquals(64, ItemConduitNetworkThrottle.resolveSlotWindow(state, 3000, 64));
        assertEquals(64, ItemConduitNetworkThrottle.resolveSlotWindow(state, 3000, 64));
        assertTrue(state.slotCursor >= 64);
    }

    @Test
    void fingerprintIsStableForSameInputs() {
        long a = ItemConduitNetworkThrottle.fingerprint(10, 1L, 2L, 3L);
        long b = ItemConduitNetworkThrottle.fingerprint(10, 1L, 2L, 3L);
        long c = ItemConduitNetworkThrottle.fingerprint(11, 1L, 2L, 3L);
        assertEquals(a, b);
        assertTrue(a != c);
    }
}
