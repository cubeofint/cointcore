package com.mawlee.cointcore.invsee;

import java.util.UUID;

/**
 * One editor per target. Idle holders lose the lock after {@code timeoutMs}.
 */
public final class InvSeeEditLock {
    public static final long DEFAULT_TIMEOUT_MS = 5L * 60L * 1000L;

    private final long timeoutMs;
    private UUID editorId;
    private String editorName;
    private long lastActionMs;

    public InvSeeEditLock() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public InvSeeEditLock(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean tryAcquire(UUID viewerId, String viewerName, long nowMs) {
        expireIfIdle(nowMs);
        if (editorId == null || editorId.equals(viewerId)) {
            editorId = viewerId;
            editorName = viewerName;
            lastActionMs = nowMs;
            return true;
        }
        return false;
    }

    public boolean release(UUID viewerId) {
        if (editorId != null && editorId.equals(viewerId)) {
            clear();
            return true;
        }
        return false;
    }

    public void forceRelease() {
        clear();
    }

    public boolean isHeldBy(UUID viewerId) {
        return editorId != null && editorId.equals(viewerId);
    }

    public boolean isHeld() {
        return editorId != null;
    }

    public UUID editorId() {
        return editorId;
    }

    public String editorName() {
        return editorName;
    }

    public void touch(UUID viewerId, long nowMs) {
        if (isHeldBy(viewerId)) {
            lastActionMs = nowMs;
        }
    }

    public boolean expireIfIdle(long nowMs) {
        if (editorId == null) {
            return false;
        }
        if (nowMs - lastActionMs >= timeoutMs) {
            clear();
            return true;
        }
        return false;
    }

    private void clear() {
        editorId = null;
        editorName = null;
        lastActionMs = 0L;
    }
}
