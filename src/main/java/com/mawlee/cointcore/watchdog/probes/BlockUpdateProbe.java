package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

/**
 * Neighbor / fluid / light / random-tick counters. Hot path is increment only.
 */
public final class BlockUpdateProbe {
    private final LongAdder neighborUpdates = new LongAdder();
    private final LongAdder fluidTicks = new LongAdder();
    private final LongAdder blockTicks = new LongAdder();
    private final LongAdder lightBacklog = new LongAdder();
    private final LongAdder randomTickSamples = new LongAdder();
    private final RankedCounter randomTickHotspots = new RankedCounter(512);

    public void incrementNeighbor() {
        neighborUpdates.increment();
    }

    public void recordScheduled(long fluids, long blocks, long light) {
        if (fluids > 0L) {
            fluidTicks.add(fluids);
        }
        if (blocks > 0L) {
            blockTicks.add(blocks);
        }
        if (light > 0L) {
            lightBacklog.add(light);
        }
    }

    public void incrementRandomTick(String dimension, int chunkX, int chunkZ) {
        randomTickSamples.increment();
        randomTickHotspots.increment(ChunkLoadProbe.coordKey(dimension, chunkX, chunkZ));
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "block updates: neighbor=%d fluids=%d scheduled=%d lightBacklog=%d randomTickSamples=%d",
                neighborUpdates.sum(),
                fluidTicks.sum(),
                blockTicks.sum(),
                lightBacklog.sum(),
                randomTickSamples.sum()
        ));
        int index = 1;
        for (RankedCounter.Rank rank : randomTickHotspots.top(top)) {
            lines.add(String.format(Locale.ROOT, "random-tick %d) %s ×%d", index++, rank.key(), rank.value()));
        }
        return lines;
    }

    public void resetWindow() {
        neighborUpdates.reset();
        fluidTicks.reset();
        blockTicks.reset();
        lightBacklog.reset();
        randomTickSamples.reset();
        randomTickHotspots.clear();
    }
}
