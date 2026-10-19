package com.mawlee.cointcore.watchdog.stall;

import java.util.ArrayList;
import java.util.List;

/**
 * Off-thread stall stage machine. No Minecraft types so unit tests stay cheap.
 */
public final class StallWatchdogStages {
    public enum Action {
        NONE,
        WARN_DUMP,
        REPEAT_DUMP,
        NOTIFY_STAFF,
        REQUEST_BREAK,
        RECOVERED
    }

    public record Settings(
            long warnAfterMs,
            long dumpIntervalMs,
            int maxDumps,
            long notifyAfterMs,
            long breakAfterMs,
            boolean breakEnabled
    ) {
    }

    private boolean inStall;
    private long stallStartMs;
    private int dumps;
    private long lastDumpMs;
    private boolean notified;
    private boolean breakRequested;
    private String lastStackKey = "";
    private int collapsedStacks;
    private long lastStallDurationMs;

    public List<Action> evaluate(long nowMs, long tickStartMs, Settings settings) {
        List<Action> actions = new ArrayList<>(4);
        if (tickStartMs <= 0L || nowMs - tickStartMs < settings.warnAfterMs()) {
            if (inStall) {
                lastStallDurationMs = nowMs - stallStartMs;
                actions.add(Action.RECOVERED);
                resetKeepingDuration();
            }
            return actions;
        }
        if (!inStall) {
            inStall = true;
            stallStartMs = tickStartMs;
        }
        long elapsed = nowMs - tickStartMs;
        if (dumps == 0) {
            actions.add(Action.WARN_DUMP);
            dumps = 1;
            lastDumpMs = nowMs;
        } else if (dumps < settings.maxDumps() && nowMs - lastDumpMs >= settings.dumpIntervalMs()) {
            actions.add(Action.REPEAT_DUMP);
            dumps++;
            lastDumpMs = nowMs;
        }
        if (!notified && elapsed >= settings.notifyAfterMs()) {
            actions.add(Action.NOTIFY_STAFF);
            notified = true;
        }
        if (!breakRequested && settings.breakEnabled() && elapsed >= settings.breakAfterMs()) {
            actions.add(Action.REQUEST_BREAK);
            breakRequested = true;
        }
        if (actions.isEmpty()) {
            actions.add(Action.NONE);
        }
        return actions;
    }

    public boolean noteStack(String key) {
        if (key != null && key.equals(lastStackKey)) {
            collapsedStacks++;
            return true;
        }
        lastStackKey = key == null ? "" : key;
        collapsedStacks = 0;
        return false;
    }

    public int collapsedStacks() {
        return collapsedStacks;
    }

    public boolean inStall() {
        return inStall;
    }

    public long stallStartMs() {
        return stallStartMs;
    }

    public long lastStallDurationMs() {
        return lastStallDurationMs;
    }

    public int dumps() {
        return dumps;
    }

    public boolean breakRequested() {
        return breakRequested;
    }

    public void reset() {
        lastStallDurationMs = 0L;
        resetKeepingDuration();
    }

    private void resetKeepingDuration() {
        inStall = false;
        stallStartMs = 0L;
        dumps = 0;
        lastDumpMs = 0L;
        notified = false;
        breakRequested = false;
        lastStackKey = "";
        collapsedStacks = 0;
    }
}
