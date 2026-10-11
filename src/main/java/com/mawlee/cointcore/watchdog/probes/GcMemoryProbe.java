package com.mawlee.cointcore.watchdog.probes;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cheap GC / heap deltas via MXBeans. Two reads per tick when enabled.
 */
public final class GcMemoryProbe {
    public record Snapshot(
            long collections,
            long pauseMs,
            double heapUsedPercent,
            long heapUsedBytes,
            long heapMaxBytes,
            int slowTicksWithLongGc,
            boolean lastSlowHadLongGc
    ) {
    }

    private final AtomicLong lastCollectionCount = new AtomicLong();
    private final AtomicLong lastCollectionTimeMs = new AtomicLong();
    private final AtomicLong windowCollections = new AtomicLong();
    private final AtomicLong windowPauseMs = new AtomicLong();
    private final AtomicLong slowTicksWithLongGc = new AtomicLong();
    private volatile boolean lastSlowHadLongGc;
    private volatile boolean primed;

    public void onTickEnd(boolean slowTick, double pauseFlagMs) {
        GcReading reading = readGc();
        if (!primed) {
            lastCollectionCount.set(reading.count());
            lastCollectionTimeMs.set(reading.timeMs());
            primed = true;
            return;
        }
        long countDelta = Math.max(0L, reading.count() - lastCollectionCount.get());
        long timeDelta = Math.max(0L, reading.timeMs() - lastCollectionTimeMs.get());
        lastCollectionCount.set(reading.count());
        lastCollectionTimeMs.set(reading.timeMs());
        if (countDelta > 0L) {
            windowCollections.addAndGet(countDelta);
            windowPauseMs.addAndGet(timeDelta);
        }
        boolean longPause = timeDelta >= Math.max(1.0D, pauseFlagMs);
        if (slowTick && longPause) {
            slowTicksWithLongGc.incrementAndGet();
            lastSlowHadLongGc = true;
        } else if (slowTick) {
            lastSlowHadLongGc = false;
        }
    }

    public Snapshot snapshot() {
        MemoryUsage heap = heapUsage();
        long used = heap == null ? 0L : heap.getUsed();
        long max = heap == null ? 0L : heap.getMax();
        if (max <= 0L && heap != null) {
            max = heap.getCommitted();
        }
        double percent = max <= 0L ? 0.0D : (used * 100.0D) / max;
        return new Snapshot(
                windowCollections.get(),
                windowPauseMs.get(),
                percent,
                used,
                max,
                (int) slowTicksWithLongGc.get(),
                lastSlowHadLongGc
        );
    }

    public void resetWindow() {
        windowCollections.set(0L);
        windowPauseMs.set(0L);
        slowTicksWithLongGc.set(0L);
        lastSlowHadLongGc = false;
    }

    public List<String> formatLines(Snapshot snapshot) {
        List<String> lines = new ArrayList<>(4);
        lines.add(String.format(
                Locale.ROOT,
                "GC: collections=%d pause=%d ms heap=%.1f%% (%s / %s)%s",
                snapshot.collections(),
                snapshot.pauseMs(),
                snapshot.heapUsedPercent(),
                humanBytes(snapshot.heapUsedBytes()),
                humanBytes(snapshot.heapMaxBytes()),
                snapshot.lastSlowHadLongGc() ? " | last slow tick coincided with long GC" : ""
        ));
        if (snapshot.slowTicksWithLongGc() > 0) {
            lines.add("slow ticks with long GC: " + snapshot.slowTicksWithLongGc());
        }
        return lines;
    }

    static String humanBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + "B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        while (value >= 1024.0D && unit < units.length - 1) {
            value /= 1024.0D;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f%s", value, units[Math.max(0, unit)]);
    }

    private static GcReading readGc() {
        long count = 0L;
        long time = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long beanCount = bean.getCollectionCount();
            long beanTime = bean.getCollectionTime();
            if (beanCount > 0L) {
                count += beanCount;
            }
            if (beanTime > 0L) {
                time += beanTime;
            }
        }
        return new GcReading(count, time);
    }

    private static MemoryUsage heapUsage() {
        try {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            return memory == null ? null : memory.getHeapMemoryUsage();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record GcReading(long count, long timeMs) {
    }
}
