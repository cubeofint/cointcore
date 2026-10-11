package com.mawlee.cointcore.watchdog.probes;

/**
 * Compact ring buffer of tick durations for 1m / 5m averages.
 */
public final class TickMsptWindow {
    private final long[] samples;
    private int writeIndex;
    private int count;

    public TickMsptWindow(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.samples = new long[capacity];
    }

    public void record(long tickNanos) {
        samples[writeIndex] = Math.max(0L, tickNanos);
        writeIndex = (writeIndex + 1) % samples.length;
        if (count < samples.length) {
            count++;
        }
    }

    public int size() {
        return count;
    }

    public double averageLastMillis(int lookback) {
        if (count == 0 || lookback <= 0) {
            return 0.0D;
        }
        int checked = Math.min(lookback, count);
        long sum = 0L;
        for (int i = 1; i <= checked; i++) {
            int index = writeIndex - i;
            if (index < 0) {
                index += samples.length;
            }
            sum += samples[index];
        }
        return sum / (double) checked / 1_000_000.0D;
    }

    public void clear() {
        writeIndex = 0;
        count = 0;
    }
}
