package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

/**
 * Chunk load / generate counters attributed to a cheap cause string.
 */
public final class ChunkLoadProbe {
    private final LongAdder loaded = new LongAdder();
    private final LongAdder generated = new LongAdder();
    private final RankedCounter causes = new RankedCounter(256);
    private final RankedCounter players = new RankedCounter(256);
    private final RankedCounter generatedChunks = new RankedCounter(512);

    public void record(String dimension, int chunkX, int chunkZ, boolean isNewChunk, String cause, String player) {
        loaded.increment();
        String reason = (cause == null || cause.isBlank()) ? "unknown" : cause;
        causes.increment(reason);
        if (!isNewChunk) {
            return;
        }
        generated.increment();
        generatedChunks.increment(coordKey(dimension, chunkX, chunkZ));
        if (player != null && !player.isBlank()) {
            players.increment(player);
        }
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "chunks: loaded=%d generated=%d",
                loaded.sum(),
                generated.sum()
        ));
        appendRanks(lines, "causes", causes.top(top));
        appendRanks(lines, "players generating", players.top(top));
        appendRanks(lines, "new chunks", generatedChunks.top(top));
        return lines;
    }

    public void resetWindow() {
        loaded.reset();
        generated.reset();
        causes.clear();
        players.clear();
        generatedChunks.clear();
    }

    static String coordKey(String dimension, int chunkX, int chunkZ) {
        String dim = dimension == null || dimension.isBlank() ? "unknown" : dimension;
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        return String.format(Locale.ROOT, "%s chunk=%d,%d pos=%d,~,%d", dim, chunkX, chunkZ, blockX, blockZ);
    }

    private static void appendRanks(List<String> lines, String title, List<RankedCounter.Rank> ranks) {
        if (ranks.isEmpty()) {
            return;
        }
        int index = 1;
        for (RankedCounter.Rank rank : ranks) {
            lines.add(String.format(Locale.ROOT, "%s %d) %s ×%d", title, index++, rank.key(), rank.value()));
        }
    }
}
