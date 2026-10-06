package com.mawlee.cointcore.justdirethings;

import java.util.UUID;

/**
 * Policy for Just Dire Things portal force-loading (testable without a running server).
 */
public final class JdtPortalChunkLoadPolicy {
    public enum Mode {
        NONE,
        OWNER_ONLINE
    }

    private JdtPortalChunkLoadPolicy() {
    }

    public static boolean shouldForceLoad(boolean enabled, Mode mode, boolean ownerOnline) {
        if (!enabled) {
            return true;
        }
        return switch (mode) {
            case NONE -> false;
            case OWNER_ONLINE -> ownerOnline;
        };
    }

    public static boolean isOwnerOnline(UUID owner, boolean playerPresent) {
        return owner != null && playerPresent;
    }
}
