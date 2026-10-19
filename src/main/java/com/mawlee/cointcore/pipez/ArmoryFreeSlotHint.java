package com.mawlee.cointcore.pipez;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;

/**
 * Per-cabinet "next free slot" hint for Pipez inserts into Functional Storage Armory Cabinets.
 * <p>
 * The cabinet holds one item per slot across 4096 slots, and {@code ItemHandlerHelper.insertItem} walks
 * every occupied slot from 0 before it finds a free one. The hint starts the search where the last insert
 * landed; the search wraps around, so slots freed below the hint are still found. A cabinet found full is
 * not rescanned for {@link #FULL_RECHECK_NANOS}.
 */
public final class ArmoryFreeSlotHint {
    static final long FULL_RECHECK_NANOS = 1_000_000_000L;
    public static final int FULL = -1;

    private static final Map<Object, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private ArmoryFreeSlotHint() {
    }

    private static final class State {
        int next;
        long fullUntilNanos;
    }

    /** First free slot at or after the cabinet's hint (wrapping), or {@link #FULL}. */
    public static int findFreeSlot(Object cabinet, int size, IntPredicate isEmpty, long nowNanos) {
        State state = STATES.computeIfAbsent(cabinet, ignored -> new State());
        if (state.fullUntilNanos != 0L && nowNanos < state.fullUntilNanos) {
            return FULL;
        }
        int slot = scan(state.next, size, isEmpty);
        state.fullUntilNanos = slot == FULL ? nowNanos + FULL_RECHECK_NANOS : 0L;
        return slot;
    }

    /** A real (non-simulated) insert filled {@code slot}; the next search starts after it. */
    public static void onInserted(Object cabinet, int slot, int size) {
        State state = STATES.get(cabinet);
        if (state != null) {
            state.next = slot + 1 >= size ? 0 : slot + 1;
        }
    }

    static int scan(int start, int size, IntPredicate isEmpty) {
        if (size <= 0) {
            return FULL;
        }
        int from = Math.floorMod(start, size);
        for (int i = from; i < size; i++) {
            if (isEmpty.test(i)) {
                return i;
            }
        }
        for (int i = 0; i < from; i++) {
            if (isEmpty.test(i)) {
                return i;
            }
        }
        return FULL;
    }

    static void resetForTests() {
        STATES.clear();
    }
}
