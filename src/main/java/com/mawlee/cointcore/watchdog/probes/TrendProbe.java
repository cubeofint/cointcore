package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1m / 5m MSPT plus BE/entity growth per claim or chunk cluster.
 */
public final class TrendProbe {
    public record BaseCount(String base, int blockEntities, int entities) {
    }

    public record BaseGrowth(String base, int blockEntities, int entities, int beDelta, int entityDelta) {
    }

    private final TickMsptWindow window = new TickMsptWindow(6_000);
    private final ConcurrentHashMap<String, int[]> lastCounts = new ConcurrentHashMap<>();
    private volatile List<BaseGrowth> lastGrowth = List.of();
    private volatile double lastOneMinuteMspt;
    private volatile double lastFiveMinuteMspt;

    public void recordTick(long tickNanos) {
        window.record(tickNanos);
        lastOneMinuteMspt = window.averageLastMillis(1_200);
        lastFiveMinuteMspt = window.averageLastMillis(6_000);
    }

    public void recordBaseCounts(List<BaseCount> counts) {
        List<BaseGrowth> growth = new ArrayList<>();
        if (counts != null) {
            for (BaseCount count : counts) {
                if (count == null || count.base() == null || count.base().isBlank()) {
                    continue;
                }
                int[] previous = lastCounts.put(count.base(), new int[] {count.blockEntities(), count.entities()});
                int prevBe = previous == null ? count.blockEntities() : previous[0];
                int prevEnt = previous == null ? count.entities() : previous[1];
                growth.add(new BaseGrowth(
                        count.base(),
                        count.blockEntities(),
                        count.entities(),
                        count.blockEntities() - prevBe,
                        count.entities() - prevEnt
                ));
            }
        }
        growth.sort(Comparator
                .comparingInt((BaseGrowth item) -> Math.abs(item.beDelta()) + Math.abs(item.entityDelta()))
                .reversed()
                .thenComparing(BaseGrowth::base));
        lastGrowth = List.copyOf(growth);
    }

    public double oneMinuteMspt() {
        return lastOneMinuteMspt;
    }

    public double fiveMinuteMspt() {
        return lastFiveMinuteMspt;
    }

    public List<BaseGrowth> growth() {
        return lastGrowth;
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "trends: avg MSPT 1m=%.2f 5m=%.2f samples=%d",
                lastOneMinuteMspt,
                lastFiveMinuteMspt,
                window.size()
        ));
        int index = 1;
        for (BaseGrowth item : lastGrowth) {
            if (index > top) {
                break;
            }
            lines.add(String.format(
                    Locale.ROOT,
                    "base %d) %s BE=%d (%+d) ent=%d (%+d)",
                    index++,
                    item.base(),
                    item.blockEntities(),
                    item.beDelta(),
                    item.entities(),
                    item.entityDelta()
            ));
        }
        return lines;
    }

    public void resetWindow() {
        // rolling averages stay; growth snapshot is replaced on the next scan
    }

    public void clear() {
        window.clear();
        lastCounts.clear();
        lastGrowth = List.of();
        lastOneMinuteMspt = 0.0D;
        lastFiveMinuteMspt = 0.0D;
    }
}
