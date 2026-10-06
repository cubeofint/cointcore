package com.mawlee.cointcore.watchdog;

import java.util.Arrays;

/**
 * Rolling window of per-tick durations (nanoseconds) with avg / p95 / max / TPS helpers.
 */
public final class TickStatsWindow {
    private final long[] samples;
    private int writeIndex;
    private int count;
    private long slowTicks;
    private final long slowThresholdNanos;

    public TickStatsWindow(int capacity, long slowThresholdNanos) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.samples = new long[capacity];
        this.slowThresholdNanos = Math.max(0L, slowThresholdNanos);
    }

    public void record(long tickNanos) {
        long value = Math.max(0L, tickNanos);
        samples[writeIndex] = value;
        writeIndex = (writeIndex + 1) % samples.length;
        if (count < samples.length) {
            count++;
        }
        if (slowThresholdNanos > 0L && value >= slowThresholdNanos) {
            slowTicks++;
        }
    }

    public int size() {
        return count;
    }

    public long slowTicks() {
        return slowTicks;
    }

    public void resetSlowTicks() {
        slowTicks = 0L;
    }

    public void clear() {
        writeIndex = 0;
        count = 0;
        slowTicks = 0L;
        Arrays.fill(samples, 0L);
    }

    public double averageMillis() {
        if (count == 0) {
            return 0.0D;
        }
        long sum = 0L;
        for (int i = 0; i < count; i++) {
            sum += samples[i];
        }
        return sum / (double) count / 1_000_000.0D;
    }

    public double maxMillis() {
        if (count == 0) {
            return 0.0D;
        }
        long max = 0L;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, samples[i]);
        }
        return max / 1_000_000.0D;
    }

    public double percentile95Millis() {
        if (count == 0) {
            return 0.0D;
        }
        long[] copy = Arrays.copyOf(samples, count);
        Arrays.sort(copy);
        int index = (int) Math.ceil(0.95D * copy.length) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= copy.length) {
            index = copy.length - 1;
        }
        return copy[index] / 1_000_000.0D;
    }

    public double estimatedTps() {
        double avgMs = averageMillis();
        if (avgMs <= 0.0D) {
            return 20.0D;
        }
        return Math.min(20.0D, 1000.0D / avgMs);
    }

    public int recentSlowCount(int lookback) {
        if (count == 0 || lookback <= 0) {
            return 0;
        }
        int checked = Math.min(lookback, count);
        int slow = 0;
        for (int i = 1; i <= checked; i++) {
            int index = writeIndex - i;
            if (index < 0) {
                index += samples.length;
            }
            if (samples[index] >= slowThresholdNanos) {
                slow++;
            }
        }
        return slow;
    }

    public boolean lastWasSlow() {
        return recentSlowCount(1) > 0;
    }
}
