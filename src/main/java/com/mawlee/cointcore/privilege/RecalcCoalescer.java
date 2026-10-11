package com.mawlee.cointcore.privilege;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At most one queued permission check per player, plus a minimum interval between runs.
 */
public final class RecalcCoalescer {
    public static final int DEFAULT_MIN_INTERVAL_TICKS = 10;

    private final int minIntervalTicks;
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public RecalcCoalescer() {
        this(DEFAULT_MIN_INTERVAL_TICKS);
    }

    public RecalcCoalescer(int minIntervalTicks) {
        this.minIntervalTicks = Math.max(1, minIntervalTicks);
    }

    /**
     * @return {@code true} if the caller should queue a check
     */
    public boolean tryQueue(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        State current = states.get(playerId);
        if (current != null && current.pending) {
            return false;
        }
        int lastRunTick = current == null ? Integer.MIN_VALUE / 2 : current.lastRunTick;
        states.put(playerId, new State(true, lastRunTick));
        return true;
    }

    /**
     * Clears the pending flag. Returns {@code true} when the min interval has elapsed
     * and the expensive check should run.
     */
    public boolean finishAndShouldRun(UUID playerId, int nowTick) {
        if (playerId == null) {
            return false;
        }
        State current = states.get(playerId);
        int lastRunTick = current == null ? Integer.MIN_VALUE / 2 : current.lastRunTick;
        boolean run = nowTick - lastRunTick >= minIntervalTicks;
        states.put(playerId, new State(false, run ? nowTick : lastRunTick));
        return run;
    }

    public void clear(UUID playerId) {
        if (playerId != null) {
            states.remove(playerId);
        }
    }

    public void reset() {
        states.clear();
    }

    public boolean isPending(UUID playerId) {
        State current = playerId == null ? null : states.get(playerId);
        return current != null && current.pending;
    }

    private record State(boolean pending, int lastRunTick) {
    }
}
