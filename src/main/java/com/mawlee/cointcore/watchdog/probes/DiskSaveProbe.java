package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

/**
 * World / SavedData save timings. Mixins only run around save, not per tick.
 */
public final class DiskSaveProbe {
    private final ThreadLocal<Long> worldSaveStart = ThreadLocal.withInitial(() -> 0L);
    private final ThreadLocal<Long> savedDataStart = ThreadLocal.withInitial(() -> 0L);
    private final LongAdder worldSaves = new LongAdder();
    private final LongAdder worldSaveNanos = new LongAdder();
    private final LongAdder questSaves = new LongAdder();
    private final LongAdder questSaveNanos = new LongAdder();
    private final RankedCounter byName = new RankedCounter(64);
    private volatile long lastWorldSaveAtMs;
    private volatile long lastWorldSaveNanos;
    private volatile String lastWorldSaveLabel = "-";

    public void onLevelSaveEvent() {
        long now = System.currentTimeMillis();
        if (now - lastWorldSaveAtMs < 2_000L) {
            return;
        }
        lastWorldSaveAtMs = now;
        lastWorldSaveLabel = "level-event";
    }

    public void onWorldSaveStart() {
        worldSaveStart.set(System.nanoTime());
    }

    public void onWorldSaveEnd(String label) {
        long started = worldSaveStart.get();
        worldSaveStart.set(0L);
        if (started == 0L) {
            lastWorldSaveAtMs = System.currentTimeMillis();
            lastWorldSaveLabel = label == null ? "level" : label;
            return;
        }
        long nanos = System.nanoTime() - started;
        worldSaves.increment();
        worldSaveNanos.add(nanos);
        lastWorldSaveAtMs = System.currentTimeMillis();
        lastWorldSaveNanos = nanos;
        lastWorldSaveLabel = label == null ? "level" : label;
        byName.increment(lastWorldSaveLabel, nanos);
    }

    public void onSavedDataStart() {
        savedDataStart.set(System.nanoTime());
    }

    public void onSavedDataEnd(String className) {
        long started = savedDataStart.get();
        savedDataStart.set(0L);
        if (started == 0L) {
            return;
        }
        long nanos = System.nanoTime() - started;
        String name = shortName(className);
        byName.increment(name, nanos);
        if (isQuestSave(className)) {
            questSaves.increment();
            questSaveNanos.add(nanos);
        }
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "saves: count=%d last=%s %.2f ms ago=%s",
                worldSaves.sum(),
                lastWorldSaveLabel,
                lastWorldSaveNanos / 1_000_000.0D,
                lastWorldSaveAtMs == 0L ? "-" : (System.currentTimeMillis() - lastWorldSaveAtMs) + "ms"
        ));
        if (questSaves.sum() > 0L) {
            lines.add(String.format(
                    Locale.ROOT,
                    "ftbquests/SavedData spikes: count=%d total=%.2f ms",
                    questSaves.sum(),
                    questSaveNanos.sum() / 1_000_000.0D
            ));
        }
        for (RankedCounter.Rank rank : byName.top(top)) {
            lines.add(String.format(Locale.ROOT, "save %s %.2f ms", rank.key(), rank.value() / 1_000_000.0D));
        }
        return lines;
    }

    public String statusLine() {
        if (lastWorldSaveAtMs == 0L) {
            return "disk: no save yet";
        }
        return String.format(
                Locale.ROOT,
                "disk: last %s %.2f ms (%ds ago)",
                lastWorldSaveLabel,
                lastWorldSaveNanos / 1_000_000.0D,
                Math.max(0L, (System.currentTimeMillis() - lastWorldSaveAtMs) / 1000L)
        );
    }

    public void resetWindow() {
        worldSaves.reset();
        worldSaveNanos.reset();
        questSaves.reset();
        questSaveNanos.reset();
        byName.clear();
    }

    static boolean isQuestSave(String className) {
        if (className == null) {
            return false;
        }
        String lower = className.toLowerCase(Locale.ROOT);
        return lower.contains("ftbquests") || lower.contains("quest");
    }

    static String shortName(String className) {
        if (className == null || className.isBlank()) {
            return "unknown";
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }
}
