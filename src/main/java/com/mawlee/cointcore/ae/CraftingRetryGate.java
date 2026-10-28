package com.mawlee.cointcore.ae;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;

/** Delays only an unchanged automatic request after submission failed, never its first attempt. */
public final class CraftingRetryGate<K> {
    private final Int2ObjectOpenHashMap<Failure<K>> failures = new Int2ObjectOpenHashMap<>();

    public boolean shouldDelay(int slot, K key, long amount, Object level, long tick, int interval) {
        var failed = failures.get(slot);
        if (failed == null) return false;
        if (interval > 1 && failed.level == level && failed.amount == amount && Objects.equals(failed.key, key)
                && tick >= failed.tick && tick - failed.tick < interval) return true;
        failures.remove(slot);
        return false;
    }

    public void failed(int slot, K key, long amount, Object level, long tick) {
        failures.put(slot, new Failure<>(key, amount, level, tick));
    }

    private record Failure<K>(K key, long amount, Object level, long tick) { }
}
