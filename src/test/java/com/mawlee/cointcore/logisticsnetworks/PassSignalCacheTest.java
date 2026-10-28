package com.mawlee.cointcore.logisticsnetworks;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PassSignalCacheTest {
    @Test void nodesOnTheSameBlockReadTheWorldOnceAndKeepTheExactSignal() {
        var cache = new PassSignalCache<Object>();
        var level = new Object();
        var reads = new AtomicInteger();
        for (int i = 0; i < 64; i++) {
            assertEquals(7, cache.get(level, 42, () -> { reads.incrementAndGet(); return 7; }));
        }
        assertEquals(1, reads.get());
        assertEquals(0, cache.get(level, 43, () -> 0));
        assertEquals(0, cache.get(level, 43, () -> { throw new AssertionError("Zero is cached too"); }));
    }

    @Test void dimensionsAndSubsequentPassesNeverShareStaleValues() {
        var cache = new PassSignalCache<Object>();
        var firstLevel = new String("same name");
        var otherLevel = new String("same name");
        assertEquals(15, cache.get(firstLevel, 0, () -> 15));
        assertEquals(0, cache.get(otherLevel, 0, () -> 0));
        assertEquals(0, new PassSignalCache<>().get(firstLevel, 0, () -> 0));
    }

    @Test void failedWorldReadsAreNotCached() {
        var cache = new PassSignalCache<Object>();
        var level = new Object();
        assertThrows(IllegalStateException.class, () -> cache.get(level, 1, () -> { throw new IllegalStateException(); }));
        assertEquals(3, cache.get(level, 1, () -> 3));
    }
}
