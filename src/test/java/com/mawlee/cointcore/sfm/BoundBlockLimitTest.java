package com.mawlee.cointcore.sfm;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class BoundBlockLimitTest {
    private static LongSet positions(long... values) { return new LongOpenHashSet(values); }

    @Test void aBlockUnderSeveralLabelsOnlyConsumesOnePosition() {
        var labels = List.of(positions(1, 2), positions(2, 3), positions(1, 3));
        assertFalse(BoundBlockLimit.exceeds(labels, 3));
        assertTrue(BoundBlockLimit.exceeds(labels, 2));
        assertFalse(BoundBlockLimit.exceeds(List.of(), 1));
    }

    @Test void zeroDisablesTheLimitWithoutScanning() {
        Iterable<LongSet> mustNotRead = () -> { throw new AssertionError("Disabled limits must not scan"); };
        assertFalse(BoundBlockLimit.exceeds(mustNotRead, 0));
        assertTrue(BoundBlockLimit.allowsReplacement(mustNotRead, mustNotRead, 0));
    }

    @Test void newOversizedBindingsAreRejectedWithoutMutatingEitherInput() {
        var old = List.of(positions(1, 2));
        var incoming = List.of(positions(1, 2, 3));
        assertFalse(BoundBlockLimit.allowsReplacement(old, incoming, 2));
        assertEquals(2, old.getFirst().size());
        assertEquals(3, incoming.getFirst().size());
        assertTrue(BoundBlockLimit.allowsReplacement(old, List.of(positions(2, 4)), 2));
    }

    @Test void grandfatheredDisksMayRemovePositionsAndRelabelButCannotAddNewPositions() {
        var old = List.of(positions(1, 2, 3, 4));
        assertTrue(BoundBlockLimit.allowsReplacement(old, List.of(positions(1, 2, 3)), 2));
        assertTrue(BoundBlockLimit.allowsReplacement(old, List.of(positions(1, 2), positions(3, 4)), 2));
        assertFalse(BoundBlockLimit.allowsReplacement(old, List.of(positions(1, 2, 5)), 2));
        assertTrue(BoundBlockLimit.allowsReplacement(old, List.of(), 2));
    }

    @Test void unchangedSnapshotsAreCachedAndConfigChangesApplyImmediately() {
        var cache = new BoundBlockLimit.Cache();
        var reads = new AtomicInteger();
        Object snapshot = new Object();
        java.util.function.Supplier<List<LongSet>> read = () -> {
            reads.incrementAndGet();
            return List.of(positions(1, 2, 3));
        };
        assertTrue(cache.exceeds(snapshot, 2, read));
        assertTrue(cache.exceeds(snapshot, 2, read));
        assertEquals(1, reads.get());
        assertFalse(cache.exceeds(snapshot, 3, read));
        assertEquals(2, reads.get());
        assertTrue(cache.exceeds(new Object(), 2, read));
        assertEquals(3, reads.get());
        assertFalse(cache.exceeds(snapshot, 0, () -> { throw new AssertionError(); }));
    }
}
