package com.mawlee.cointcore.ae2;

import appeng.api.stacks.KeyCounter;

import java.util.function.Consumer;

/** Server-thread snapshots shared by readers of a network, never by insertion/extraction operations. */
public final class NetworkInventoryScanCache {
    // An exposed subnetwork can be changed without passing through its parent network. A shared revision
    // conservatively invalidates parent snapshots as well, without retaining a graph of storage dependencies.
    private static long revision;
    private static final ThreadLocal<Scan> ACTIVE_SCAN = new ThreadLocal<>();

    private Object server;
    private int tick;
    private long snapshotRevision;
    private KeyCounter snapshot;

    public static void invalidate() { revision++; }

    public void clear() { snapshot = null; server = null; }

    public void read(Object server, int tick, boolean mountsInUse, KeyCounter out, Consumer<KeyCounter> scan) {
        Scan active = ACTIVE_SCAN.get();
        if (mountsInUse || active != null) {
            // Keep AE2's recursion guard. A nested scan is context-dependent, so never memoize it.
            if (mountsInUse && active != null) active.recursive = true;
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
        var context = new Scan();
        ACTIVE_SCAN.set(context);
        try {
            scan.accept(candidate);
        } finally {
            ACTIVE_SCAN.remove();
            // The caller owns its accumulator, including any entries present before this scan.
            out.addAll(candidate);
        }
        if (!context.recursive && before == revision) {
            this.server = server;
            this.tick = tick;
            snapshotRevision = before;
            snapshot = candidate;
        }
    }

    private static final class Scan {
        private boolean recursive;
    }
}
