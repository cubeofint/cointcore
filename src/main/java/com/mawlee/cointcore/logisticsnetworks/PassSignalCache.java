package com.mawlee.cointcore.logisticsnetworks;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.IdentityHashMap;
import java.util.function.IntSupplier;

/** Exact signals shared only during one read-only preparation pass; never retained across transfers. */
public final class PassSignalCache<L> {
    private final IdentityHashMap<L, Long2IntOpenHashMap> levels = new IdentityHashMap<>();

    public int get(L level, long position, IntSupplier query) {
        var signals = levels.get(level);
        if (signals == null) {
            signals = new Long2IntOpenHashMap();
            signals.defaultReturnValue(-1);
            levels.put(level, signals);
        }
        int signal = signals.get(position);
        if (signal >= 0) return signal;
        signal = query.getAsInt();
        signals.put(position, signal);
        return signal;
    }
}
