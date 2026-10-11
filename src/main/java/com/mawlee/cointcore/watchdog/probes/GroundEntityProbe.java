package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodic ground-item / XP / projectile counts per chunk.
 */
public final class GroundEntityProbe {
    public record ChunkCount(String key, int items, int xp, int projectiles) {
        public int total() {
            return items + xp + projectiles;
        }
    }

    private final ConcurrentHashMap<String, int[]> lastScan = new ConcurrentHashMap<>();
    private volatile int lastItemTotal;
    private volatile int lastXpTotal;
    private volatile int lastProjectileTotal;

    public void replaceScan(Map<String, int[]> counts) {
        lastScan.clear();
        int items = 0;
        int xp = 0;
        int projectiles = 0;
        if (counts != null) {
            for (Map.Entry<String, int[]> entry : counts.entrySet()) {
                int[] value = entry.getValue();
                if (value == null || value.length < 3) {
                    continue;
                }
                lastScan.put(entry.getKey(), new int[] {value[0], value[1], value[2]});
                items += value[0];
                xp += value[1];
                projectiles += value[2];
            }
        }
        lastItemTotal = items;
        lastXpTotal = xp;
        lastProjectileTotal = projectiles;
    }

    public List<ChunkCount> top(int limit) {
        List<ChunkCount> ranks = new ArrayList<>(lastScan.size());
        lastScan.forEach((key, value) -> ranks.add(new ChunkCount(key, value[0], value[1], value[2])));
        ranks.sort(Comparator.comparingInt(ChunkCount::total).reversed().thenComparing(ChunkCount::key));
        if (ranks.size() > limit) {
            return List.copyOf(ranks.subList(0, limit));
        }
        return List.copyOf(ranks);
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "ground: items=%d xp=%d projectiles=%d",
                lastItemTotal,
                lastXpTotal,
                lastProjectileTotal
        ));
        int index = 1;
        for (ChunkCount count : top(top)) {
            lines.add(String.format(
                    Locale.ROOT,
                    "ground %d) %s items=%d xp=%d proj=%d",
                    index++,
                    count.key(),
                    count.items(),
                    count.xp(),
                    count.projectiles()
            ));
        }
        return lines;
    }

    public void resetWindow() {
        // keep last scan for status; counts are a snapshot, not a window total
    }
}
