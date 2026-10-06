package com.mawlee.cointcore.afk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-player AFK timers armed from the last strong activity moment (not polled idle).
 */
public final class AfkTracker {
    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_DUE_MS = new AtomicLong(Long.MAX_VALUE);

    private AfkTracker() {
    }

    public static PlayerState getOrCreate(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new PlayerState());
    }

    public static PlayerState get(UUID playerId) {
        return STATES.get(playerId);
    }

    public static boolean isMarked(UUID playerId) {
        PlayerState state = STATES.get(playerId);
        return state != null && state.marked;
    }

    public static boolean isMarked(ServerPlayer player) {
        return isMarked(player.getUUID());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
        recomputeNextDue();
    }

    public static void clearAll() {
        STATES.clear();
        NEXT_DUE_MS.set(Long.MAX_VALUE);
    }

    public static long nextDueMs() {
        return NEXT_DUE_MS.get();
    }

    public static Iterable<Map.Entry<UUID, PlayerState>> entries() {
        return STATES.entrySet();
    }

    static void noteDue(long dueMs) {
        if (dueMs <= 0L || dueMs == Long.MAX_VALUE) {
            return;
        }
        NEXT_DUE_MS.accumulateAndGet(dueMs, Math::min);
    }

    static void recomputeNextDue() {
        long next = Long.MAX_VALUE;
        for (PlayerState state : STATES.values()) {
            if (!state.armed) {
                continue;
            }
            if (!state.marked && state.markDueMs > 0L) {
                next = Math.min(next, state.markDueMs);
            }
            if (state.kickDueMs > 0L) {
                next = Math.min(next, state.kickDueMs);
            }
        }
        NEXT_DUE_MS.set(next);
    }

    public static final class PlayerState {
        long lastActivityMs;
        long markDueMs;
        long kickDueMs;
        boolean armed;
        float lastYaw;
        float lastPitch;
        boolean hasLookSample;
        boolean marked;
        int weakActionsWhileAfk;
        long lastSuspectNotifyMs;

        PlayerState() {
        }

        /**
         * Starts / restarts AFK timers from {@code nowMs} after strong activity.
         */
        public void arm(long nowMs, int markAfterSeconds, int kickAfterSeconds) {
            lastActivityMs = nowMs;
            markDueMs = nowMs + markAfterSeconds * 1000L;
            kickDueMs = nowMs + kickAfterSeconds * 1000L;
            armed = true;
            marked = false;
            weakActionsWhileAfk = 0;
            lastSuspectNotifyMs = 0L;
            noteDue(markDueMs);
            noteDue(kickDueMs);
        }

        public void disarm() {
            armed = false;
            markDueMs = 0L;
            kickDueMs = 0L;
            marked = false;
            weakActionsWhileAfk = 0;
            lastSuspectNotifyMs = 0L;
        }

        public boolean updateLook(float yaw, float pitch, double thresholdDegrees) {
            if (!hasLookSample) {
                lastYaw = yaw;
                lastPitch = pitch;
                hasLookSample = true;
                return false;
            }

            float yawDelta = Math.abs(Mth.wrapDegrees(yaw - lastYaw));
            float pitchDelta = Math.abs(pitch - lastPitch);
            lastYaw = yaw;
            lastPitch = pitch;
            return yawDelta >= thresholdDegrees || pitchDelta >= thresholdDegrees;
        }
    }
}
