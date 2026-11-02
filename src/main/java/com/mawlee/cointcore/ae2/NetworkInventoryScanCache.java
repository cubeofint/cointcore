package com.mawlee.cointcore.ae2;

import appeng.api.stacks.KeyCounter;

import java.util.function.Consumer;

/** Server-thread snapshots shared by readers of a network, never by insertion/extraction operations. */
public final class NetworkInventoryScanCache {
    // An exposed subnetwork can be changed without passing through its parent network. A shared revision
    // conservatively invalidates parent snapshots as well, without retaining a graph of storage dependencies.
    private static long revision;
    // Keep one holder per thread: removing/recreating the entry for every inventory scan
    // needlessly churns ThreadLocalMap and allocates a Scan even when no recursion occurs.
    private static final ThreadLocal<Scan> ACTIVE_SCAN = ThreadLocal.withInitial(Scan::new);

    private Object server;
    private int tick;
    private long snapshotRevision;
    private KeyCounter snapshot;

    public static void invalidate() { revision++; }

    public void clear() { snapshot = null; server = null; }

    public void read(Object server, int tick, boolean mountsInUse, KeyCounter out, Consumer<KeyCounter> scan) {
        Scan active = ACTIVE_SCAN.get();
        if (mountsInUse || active.active) {
            // Keep AE2's recursion guard. A nested scan is context-dependent, so never memoize it.
            if (mountsInUse && active.active) active.recursive = true;
            scan.accept(out);
            return;
        }
        if (snapshot != null && this.server == server && this.tick == tick && snapshotRevision == revision) {
            out.addAll(snapshot);
            return;
        }

        clear();
        long before = revision;
        var candidate = new KeyCounter();
        active.active = true;
        active.recursive = false;
        try {
            scan.accept(candidate);
        } finally {
            active.active = false;
            // The caller owns its accumulator, including any entries present before this scan.
            out.addAll(candidate);
        }
        if (!active.recursive && before == revision) {
            this.server = server;
            this.tick = tick;
            snapshotRevision = before;
            snapshot = candidate;
        }
    }

    private static final class Scan {
        private boolean active;
        private boolean recursive;
    }
}
