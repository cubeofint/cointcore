package com.mawlee.cointcore.watchdog;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PublishedWatchdogContextTest {
    @Test void contextClearsOldDamageDetailsAndReleasesReferencesOnExit() {
        var slot = new PublishedWatchdogContext();
        slot.enter(3, 7, "mob", "nether", 1, 2, 3, "player", "sword");
        assertEquals("sword", slot.snapshot().weapon());
        slot.enter(2, 0, "machine", "overworld", 4, 5, 6, "", "");
        var current = slot.snapshot();
        assertEquals(2, current.kind());
        assertEquals("", current.actor());
        assertEquals("", current.weapon());
        assertTrue(slot.isCurrent(2, 0));
        slot.exit();
        assertEquals(0, slot.snapshot().kind());
        assertNull(slot.snapshot().type());
    }

    @Test void readerNeverCombinesDifferentPublishedObjects() throws Exception {
        var slot = new PublishedWatchdogContext();
        var started = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var writer = executor.submit(() -> {
                started.countDown();
                for (int i = 1; i <= 500_000; i++) {
                    boolean first = (i & 1) == 0;
                    slot.enter(first ? 1 : 2, first ? 11 : 22, first ? "first" : "second",
                            first ? "overworld" : "nether", first ? 1 : 4, first ? 2 : 5,
                            first ? 3 : 6, first ? "a" : "b", first ? "x" : "y");
                    if ((i & 31) == 0) slot.exit();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            do {
                var value = slot.snapshot();
                if (value.kind() == 0) continue;
                boolean first = value.kind() == 1;
                assertEquals(first ? 11 : 22, value.entityId());
                assertEquals(first ? "first" : "second", value.type());
                assertEquals(first ? "overworld" : "nether", value.dimension());
                assertEquals(first ? 1 : 4, value.x());
                assertEquals(first ? 2 : 5, value.y());
                assertEquals(first ? 3 : 6, value.z());
                assertEquals(first ? "a" : "b", value.actor());
                assertEquals(first ? "x" : "y", value.weapon());
            } while (!writer.isDone());
            writer.get(5, TimeUnit.SECONDS);
        }
    }

    @Test void readerDoesNotWaitForInterruptedPublication() throws Exception {
        var slot = new PublishedWatchdogContext();
        var sequence = PublishedWatchdogContext.class.getDeclaredField("sequence");
        sequence.setAccessible(true);
        sequence.setLong(slot, 1L);
        assertEquals(0, slot.snapshot().kind());
    }

    @Test void tickProbeRetainsItsLabelAndClearsBetweenSamples() {
        try {
            TickProbe.setLabels(WatchdogLabels.STRINGS);
            TickProbe.enter("damage", "minecraft:zombie", "minecraft:overworld", 10, 20, 30);
            var value = TickProbe.snapshot();
            assertTrue(value.active());
            assertEquals("damage", value.kind());
            assertEquals("minecraft:zombie", value.typeId());
            assertEquals(20, value.y());
            TickProbe.exit();
            assertFalse(TickProbe.snapshot().active());
        } finally {
            TickProbe.clear();
        }
    }
}
