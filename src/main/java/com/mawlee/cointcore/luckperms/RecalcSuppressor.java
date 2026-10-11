package com.mawlee.cointcore.luckperms;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drops {@code UserDataRecalculateEvent}s caused by our own LuckPerms writes
 * and prevents overlapping per-player prefix reconciles.
 */
public final class RecalcSuppressor {
    public static final long DEFAULT_WINDOW_MS = 2000L;

    private static final ConcurrentHashMap<UUID, Long> SUPPRESS_UNTIL_MS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();

    private RecalcSuppressor() {
    }

    public static boolean shouldIgnoreRecalc(UUID playerId, long nowMs) {
        if (playerId == null) {
            return true;
        }
        if (IN_FLIGHT.containsKey(playerId)) {
            return true;
        }
        Long until = SUPPRESS_UNTIL_MS.get(playerId);
        return until != null && nowMs < until;
    }

    public static boolean tryBegin(UUID playerId) {
        return playerId != null && IN_FLIGHT.putIfAbsent(playerId, Boolean.TRUE) == null;
    }

    public static void end(UUID playerId) {
        if (playerId != null) {
            IN_FLIGHT.remove(playerId);
        }
    }

    public static void markOwnWrite(UUID playerId, long nowMs) {
        markOwnWrite(playerId, nowMs, DEFAULT_WINDOW_MS);
    }

    public static void markOwnWrite(UUID playerId, long nowMs, long windowMs) {
        if (playerId == null) {
            return;
        }
        SUPPRESS_UNTIL_MS.put(playerId, nowMs + Math.max(0L, windowMs));
    }

    public static void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }
        SUPPRESS_UNTIL_MS.remove(playerId);
        IN_FLIGHT.remove(playerId);
    }

    public static void reset() {
        SUPPRESS_UNTIL_MS.clear();
        IN_FLIGHT.clear();
    }
}
