package com.mawlee.cointcore.sfm;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Supplier;

/** Positions, rather than label names or label-position pairs, consume the budget. */
public final class BoundBlockLimit {
    private BoundBlockLimit() { }

    public static boolean exceeds(Iterable<? extends LongSet> groups, int limit) {
        if (limit <= 0) return false;
        var unique = new LongOpenHashSet();
        for (var group : groups) {
            var positions = group.iterator();
            while (positions.hasNext()) {
                unique.add(positions.nextLong());
                if (unique.size() > limit) return true;
            }
        }
        return false;
    }

    public static boolean allowsReplacement(Iterable<? extends LongSet> current,
                                             Iterable<? extends LongSet> incoming, int limit) {
        if (!exceeds(incoming, limit)) return true;
        // Existing oversized disks can still shed positions or change labels on the same positions.
        // They remain paused until they meet the limit; no saved labels are deleted automatically.
        var existing = new LongOpenHashSet();
        for (var group : current) existing.addAll(group);
        for (var group : incoming) if (!existing.containsAll(group)) return false;
        return true;
    }

    /** SFM replaces the saved component snapshot on each save, so unchanged disks need no rescans. */
    public static final class Cache {
        private Object snapshot;
        private int limit = -1;
        private boolean exceeded;

        public boolean exceeds(Object snapshot, int limit, Supplier<? extends Iterable<? extends LongSet>> groups) {
            if (limit <= 0) {
                this.snapshot = null;
                this.limit = limit;
                exceeded = false;
                return false;
            }
            if (this.snapshot != snapshot || this.limit != limit) {
                exceeded = BoundBlockLimit.exceeds(groups.get(), limit);
                this.snapshot = snapshot;
                this.limit = limit;
            }
            return exceeded;
        }
    }
}
