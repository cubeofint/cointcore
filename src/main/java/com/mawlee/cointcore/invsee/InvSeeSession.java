package com.mawlee.cointcore.invsee;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class InvSeeSession {
    private final UUID viewerId;
    private final InvSeeTarget target;
    private boolean editMode;
    private boolean closed;

    InvSeeSession(ServerPlayer viewer, InvSeeTarget target) {
        this.viewerId = viewer.getUUID();
        this.target = target;
        this.editMode = false;
    }

    public UUID viewerId() {
        return viewerId;
    }

    public InvSeeTarget target() {
        return target;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (target.isOffline()) {
            MinecraftServer server = target.getPlayer().getServer();
            if (server != null) {
                target.saveOffline(server);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
