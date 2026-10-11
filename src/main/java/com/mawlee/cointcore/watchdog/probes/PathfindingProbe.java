package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

/**
 * Approximate pathfinding cost: count PathNavigation recomputes per chunk.
 */
public final class PathfindingProbe {
    private final LongAdder recomputes = new LongAdder();
    private final RankedCounter perChunk = new RankedCounter(512);

    public void increment(String dimension, int chunkX, int chunkZ) {
        recomputes.increment();
        perChunk.increment(ChunkLoadProbe.coordKey(dimension, chunkX, chunkZ));
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add("pathfinding recomputes: " + recomputes.sum());
        int index = 1;
        for (RankedCounter.Rank rank : perChunk.top(top)) {
            lines.add(String.format(Locale.ROOT, "path %d) %s ×%d", index++, rank.key(), rank.value()));
        }
        return lines;
    }

    public void resetWindow() {
        recomputes.reset();
        perChunk.clear();
    }
}
