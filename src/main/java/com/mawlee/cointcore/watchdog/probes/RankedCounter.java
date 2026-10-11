package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bounded concurrent counters for probe hot paths (increment only).
 */
public final class RankedCounter {
    public record Rank(String key, long value) {
    }

    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final int maxKeys;

    public RankedCounter(int maxKeys) {
        this.maxKeys = Math.max(8, maxKeys);
    }

    public void increment(String key) {
        increment(key, 1L);
    }

    public void increment(String key, long delta) {
        if (key == null || key.isBlank() || delta == 0L) {
            return;
        }
        LongAdder adder = counts.get(key);
        if (adder == null) {
            if (counts.size() >= maxKeys) {
                return;
            }
            adder = counts.computeIfAbsent(key, ignored -> new LongAdder());
        }
        adder.add(delta);
    }

    public List<Rank> top(int limit) {
        int cap = Math.max(0, limit);
        if (cap == 0 || counts.isEmpty()) {
            return List.of();
        }
        List<Rank> ranks = new ArrayList<>(counts.size());
        counts.forEach((key, adder) -> {
            long value = adder.sum();
            if (value != 0L) {
                ranks.add(new Rank(key, value));
            }
        });
        ranks.sort(Comparator.comparingLong(Rank::value).reversed().thenComparing(Rank::key));
        if (ranks.size() > cap) {
            return List.copyOf(ranks.subList(0, cap));
        }
        return List.copyOf(ranks);
    }

    public long total() {
        long sum = 0L;
        for (LongAdder adder : counts.values()) {
            sum += adder.sum();
        }
        return sum;
    }

    public int size() {
        return counts.size();
    }

    public void clear() {
        counts.clear();
    }
}
