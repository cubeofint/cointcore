package com.mawlee.cointcore.damage;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/**
 * Per-tick hit counts and thread-local nesting depth. Keys are entity ids so
 * dead entities can be collected. Reset clears only ids touched this tick.
 */
public final class DamageGuardCounters {
    private final Int2IntOpenHashMap hits = new Int2IntOpenHashMap();
    private final IntArrayList touched = new IntArrayList();
    private final IntOpenHashSet blocked = new IntOpenHashSet();
    private final ThreadLocal<int[]> depth = ThreadLocal.withInitial(() -> new int[1]);

    public DamageGuardCounters() {
        hits.defaultReturnValue(0);
    }

    public int enter() {
        int[] slot = depth.get();
        return ++slot[0];
    }

    public void exit() {
        int[] slot = depth.get();
        if (slot[0] > 0) {
            slot[0]--;
        }
    }

    public int currentDepth() {
        return depth.get()[0];
    }

    /** Zero this thread's depth; returns what it was (non-zero means an exit was skipped). */
    public int resetDepth() {
        int[] slot = depth.get();
        int previous = slot[0];
        slot[0] = 0;
        return previous;
    }

    public int recordHit(int targetId) {
        int prev = hits.get(targetId);
        if (prev == 0) {
            touched.add(targetId);
        }
        int next = prev + 1;
        hits.put(targetId, next);
        return next;
    }

    public int hits(int targetId) {
        return hits.get(targetId);
    }

    public void markBlocked(int targetId) {
        blocked.add(targetId);
    }

    public boolean isBlocked(int targetId) {
        return blocked.contains(targetId);
    }

    public boolean shouldBlock(int targetId, int depthValue, int maxDepth, int maxHits) {
        return depthValue > maxDepth || hits.get(targetId) > maxHits || blocked.contains(targetId);
    }

    public void resetTick() {
        for (int i = 0, n = touched.size(); i < n; i++) {
            hits.remove(touched.getInt(i));
        }
        touched.clear();
        blocked.clear();
    }

    public int touchedCount() {
        return touched.size();
    }
}
