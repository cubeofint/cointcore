package com.mawlee.cointcore.logisticsnetworks;

import java.util.LinkedHashSet;
import java.util.Set;

/** Tick budget with FIFO service for deferred networks; retries never bypass an exhausted budget. */
public final class FairDispatchBudget<T> {
    private final LinkedHashSet<T> deferred = new LinkedHashSet<>();
    private long tick = Long.MIN_VALUE;
    private long startNanos;
    private boolean started;

    public Set<T> order(Set<T> pending) {
        deferred.retainAll(pending);
        var ordered = new LinkedHashSet<T>(deferred);
        ordered.addAll(pending);
        return ordered;
    }

    public boolean tryStart(T network, long gameTime, long nowNanos, long budgetNanos) {
        if (tick != gameTime) {
            tick = gameTime;
            started = false;
        }
        if (!started || nowNanos - startNanos < budgetNanos) {
            if (!started) startNanos = nowNanos;
            started = true;
            deferred.remove(network);
            return true;
        }
        deferred.add(network);
        return false;
    }

    public void clear() {
        deferred.clear();
        tick = Long.MIN_VALUE;
        started = false;
    }
}
