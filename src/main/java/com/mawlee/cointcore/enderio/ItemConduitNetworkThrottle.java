package com.mawlee.cointcore.enderio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure throttle state for EnderIO item conduit networks (unit-testable without Minecraft).
 */
public final class ItemConduitNetworkThrottle {
    public enum Decision {
        RUN,
        SKIP_INTERVAL,
        SKIP_IDLE_BACKOFF,
        SKIP_UNCHANGED
    }

    public static final class State {
        long nextAllowedGameTime;
        long lastFingerprint = Long.MIN_VALUE;
        boolean lastPassMoved;
        int slotCursor;
    }

    private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    private ItemConduitNetworkThrottle() {
    }

    public static State stateFor(int networkIdentity) {
        return STATES.computeIfAbsent(networkIdentity, ignored -> new State());
    }

    public static void clearAll() {
        STATES.clear();
    }

    /**
     * @param gameTime              current level game time
     * @param tickInterval          minimum ticks between active passes
     * @param idleBackoffTicks      delay after an idle pass
     * @param fingerprint           inventory content fingerprint for this network
     * @param skipUnchanged         whether to skip when fingerprint matches last idle pass
     */
    public static Decision shouldRun(
            State state,
            long gameTime,
            int tickInterval,
            int idleBackoffTicks,
            long fingerprint,
            boolean skipUnchanged
    ) {
        if (gameTime < state.nextAllowedGameTime) {
            if (!state.lastPassMoved && idleBackoffTicks > tickInterval) {
                return Decision.SKIP_IDLE_BACKOFF;
            }
            return Decision.SKIP_INTERVAL;
        }
        if (skipUnchanged
                && !state.lastPassMoved
                && state.lastFingerprint != Long.MIN_VALUE
                && state.lastFingerprint == fingerprint) {
            state.nextAllowedGameTime = gameTime + Math.max(tickInterval, idleBackoffTicks);
            return Decision.SKIP_UNCHANGED;
        }
        return Decision.RUN;
    }

    public static void onPassComplete(
            State state,
            long gameTime,
            boolean movedAnything,
            int tickInterval,
            int idleBackoffTicks,
            long fingerprint
    ) {
        state.lastPassMoved = movedAnything;
        state.lastFingerprint = fingerprint;
        int delay = movedAnything ? Math.max(1, tickInterval) : Math.max(tickInterval, idleBackoffTicks);
        state.nextAllowedGameTime = gameTime + delay;
    }

    /**
     * Advances a fair slot cursor and returns how many slots this pass may scan.
     */
    public static int resolveSlotWindow(State state, int totalSlots, int maxSlotsPerPass) {
        if (totalSlots <= 0) {
            state.slotCursor = 0;
            return 0;
        }
        int cap = Math.max(1, maxSlotsPerPass);
        if (totalSlots <= cap) {
            state.slotCursor = 0;
            return totalSlots;
        }
        int start = Math.floorMod(state.slotCursor, totalSlots);
        state.slotCursor = start + cap;
        return cap;
    }

    public static int slotStart(State state, int totalSlots, int maxSlotsPerPass) {
        if (totalSlots <= 0) {
            return 0;
        }
        if (totalSlots <= Math.max(1, maxSlotsPerPass)) {
            return 0;
        }
        return Math.floorMod(state.slotCursor - Math.max(1, maxSlotsPerPass), totalSlots);
    }

    /**
     * Cheap fingerprint: slot count mixed with sampled stack identities.
     * Callers supply precomputed sample hashes to stay game-agnostic.
     */
    public static long fingerprint(int slotCount, long sampleA, long sampleB, long sampleC) {
        long hash = 0x9E3779B97F4A7C15L;
        hash = mix(hash, slotCount);
        hash = mix(hash, sampleA);
        hash = mix(hash, sampleB);
        hash = mix(hash, sampleC);
        return hash;
    }

    private static long mix(long hash, long value) {
        hash ^= value + 0x9E3779B97F4A7C15L + (hash << 6) + (hash >>> 2);
        return hash;
    }
}
