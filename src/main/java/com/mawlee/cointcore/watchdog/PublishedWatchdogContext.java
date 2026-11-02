package com.mawlee.cointcore.watchdog;

import java.lang.invoke.VarHandle;

/**
 * Single-writer diagnostic slot. The server publishes without allocating or locking;
 * readers resolve registry names only after obtaining a consistent copy.
 */
public final class PublishedWatchdogContext {
    public record Snapshot(int kind, String label, int entityId, Object type, Object dimension,
                           int x, int y, int z, String actor, String weapon) {
        private static final Snapshot EMPTY = new Snapshot(0, "", 0, null, null, 0, 0, 0, "", "");
    }

    private volatile long sequence;
    private int kind, entityId, x, y, z;
    private Object type, dimension;
    private String actor = "", weapon = "";
    private String label = "";

    public void enter(int kind, int entityId, Object type, Object dimension,
                      int x, int y, int z, String actor, String weapon) {
        enter(kind, "", entityId, type, dimension, x, y, z, actor, weapon);
    }

    public void enter(int kind, String label, int entityId, Object type, Object dimension,
                      int x, int y, int z, String actor, String weapon) {
        long start = sequence;
        sequence = start + 1;
        // Keep the in-progress marker before every payload store.
        VarHandle.storeStoreFence();
        this.kind = kind;
        this.label = label;
        this.entityId = entityId;
        this.type = type;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.actor = actor;
        this.weapon = weapon;
        sequence = start + 2;
    }

    /** Only the publishing thread may inspect these unshared fields directly. */
    public boolean isCurrent(int kind, int entityId) {
        return this.kind == kind && this.entityId == entityId;
    }

    public boolean isCurrentKind(int kind) { return this.kind == kind; }

    public void exit() {
        if (kind != 0) clear();
    }

    public void clear() {
        enter(0, 0, null, null, 0, 0, 0, "", "");
    }

    public Snapshot snapshot() {
        // Diagnostics must not wait for a publishing thread that is itself stalled.
        for (int attempt = 0; attempt < 4; attempt++) {
            long before = sequence;
            if ((before & 1) != 0) continue;
            int k = kind, id = entityId, px = x, py = y, pz = z;
            Object t = type, d = dimension;
            String a = actor, w = weapon, l = label;
            VarHandle.acquireFence();
            if (sequence == before) {
                return k == 0 ? Snapshot.EMPTY : new Snapshot(k, l, id, t, d, px, py, pz, a, w);
            }
        }
        return Snapshot.EMPTY;
    }
}
