package com.mawlee.cointcore.shop;

/**
 * Ring buffer of recent buy prices for one trader offer. Oldest sample is first in {@link #snapshot()}.
 */
public final class OfferBuyPriceHistory {
    public static final int DEFAULT_CAPACITY = 48;

    private final int capacity;
    private final long[] ring;
    private int size;
    private int next;

    public OfferBuyPriceHistory(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.ring = new long[this.capacity];
    }

    public void record(long buyPrice) {
        if (buyPrice <= 0L) {
            return;
        }
        ring[next] = buyPrice;
        next = (next + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Chronological samples, oldest first. Length is {@link #size()}.
     */
    public long[] snapshot() {
        long[] out = new long[size];
        int start = size == capacity ? next : 0;
        for (int index = 0; index < size; index++) {
            out[index] = ring[(start + index) % capacity];
        }
        return out;
    }

    public static OfferBuyPriceHistory fromSnapshot(long[] values, int capacity) {
        OfferBuyPriceHistory history = new OfferBuyPriceHistory(capacity);
        if (values == null) {
            return history;
        }
        int from = Math.max(0, values.length - history.capacity);
        for (int index = from; index < values.length; index++) {
            history.record(values[index]);
        }
        return history;
    }
}
