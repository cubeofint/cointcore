package com.mawlee.cointcore.ae2;

import appeng.api.stacks.KeyCounter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class NetworkInventoryScanCacheTest {
    @Test void invalidationDuringScanDoesNotPublishAndTheReusableGuardRecoversAfterFailure() {
        var cache = new NetworkInventoryScanCache();
        var server = new Object();
        var calls = new AtomicInteger();
        Consumer<KeyCounter> changing = out -> {
            calls.incrementAndGet();
            NetworkInventoryScanCache.invalidate();
        };
        cache.read(server, 1, false, new KeyCounter(), changing);
        cache.read(server, 1, false, new KeyCounter(), changing);
        assertEquals(2, calls.get());
        assertThrows(IllegalArgumentException.class, () -> cache.read(server, 1, false, new KeyCounter(), out -> {
            throw new IllegalArgumentException("storage failed");
        }));
        for (int tick = 2; tick < 50; tick++) {
            cache.read(server, tick, false, new KeyCounter(), out -> calls.incrementAndGet());
            cache.read(server, tick, false, new KeyCounter(), out -> fail("expected cached result"));
        }
        assertEquals(50, calls.get());
    }

    @Test void evenEmptyInventoryScansAreSharedOnlyWithinTheCurrentTickAndServer() {
        var cache = new NetworkInventoryScanCache();
        var server = new Object();
        var calls = new AtomicInteger();
        Consumer<KeyCounter> scan = out -> calls.incrementAndGet();
        for (int i = 0; i < 100; i++) cache.read(server, 1, false, new KeyCounter(), scan);
        assertEquals(1, calls.get());
        cache.read(server, 2, false, new KeyCounter(), scan);
        cache.read(new Object(), 2, false, new KeyCounter(), scan);
        assertEquals(3, calls.get());
    }

    @Test void mutationOfAnExposedSubnetworkAlsoInvalidatesTheParentSnapshot() {
        var parent = new NetworkInventoryScanCache();
        var server = new Object();
        var calls = new AtomicInteger();
        Consumer<KeyCounter> scan = out -> calls.incrementAndGet();
        parent.read(server, 1, false, new KeyCounter(), scan);
        NetworkInventoryScanCache.invalidate();
        parent.read(server, 1, false, new KeyCounter(), scan);
        assertEquals(2, calls.get());
    }

    @Test void recursiveAndFailedScansCannotPublishPartialSnapshotsOrLeaveAnActiveScan() {
        var cache = new NetworkInventoryScanCache();
        var server = new Object();
        var calls = new AtomicInteger();
        Consumer<KeyCounter> recursive = out -> {
            calls.incrementAndGet();
            cache.read(server, 1, true, out, ignored -> { });
        };
        cache.read(server, 1, false, new KeyCounter(), recursive);
        cache.read(server, 1, false, new KeyCounter(), recursive);
        assertEquals(2, calls.get());
        assertThrows(IllegalStateException.class, () -> cache.read(server, 1, false, new KeyCounter(), out -> {
            throw new IllegalStateException("failed external storage");
        }));
        Consumer<KeyCounter> valid = out -> calls.incrementAndGet();
        cache.read(server, 1, false, new KeyCounter(), valid);
        cache.read(server, 1, false, new KeyCounter(), valid);
        assertEquals(3, calls.get());
    }
}
