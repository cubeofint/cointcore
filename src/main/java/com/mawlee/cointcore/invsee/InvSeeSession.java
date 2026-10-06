package com.mawlee.cointcore.invsee;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class InvSeeSession {
    private final UUID viewerId;
    private final String viewerName;
    private final InvSeeTarget target;
    private final InvSeeSection section;
    private boolean closed;

    InvSeeSession(ServerPlayer viewer, InvSeeTarget target, InvSeeSection section) {
        this.viewerId = viewer.getUUID();
        this.viewerName = viewer.getGameProfile().getName();
        this.target = target;
        this.section = section;
    }

    public UUID viewerId() {
        return viewerId;
    }

    public String viewerName() {
        return viewerName;
    }

    public InvSeeTarget target() {
        return target;
    }

    public InvSeeSection section() {
        return section;
    }

    public boolean isEditMode() {
        return target.editLock().isHeldBy(viewerId);
    }

    public boolean tryEnterEdit(ServerPlayer viewer, long nowMs) {
        return target.editLock().tryAcquire(viewer.getUUID(), viewer.getGameProfile().getName(), nowMs);
    }

    public void exitEdit() {
        target.editLock().release(viewerId);
    }

    void abandonMenu() {
        closed = true;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        target.editLock().release(viewerId);
        InvSeeAuditLog.closed(viewerName, target.displayName(), section);
        InvSeeTargets.release(target);
    }

    public boolean isClosed() {
        return closed;
    }
}
