package com.mawlee.cointcore.justdirethings;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtPortalChunkLoadPolicyTest {
    @Test
    void disabledKeepsVanillaForceLoad() {
        assertTrue(JdtPortalChunkLoadPolicy.shouldForceLoad(
                false, JdtPortalChunkLoadPolicy.Mode.NONE, false
        ));
    }

    @Test
    void noneModeNeverForceLoads() {
        assertFalse(JdtPortalChunkLoadPolicy.shouldForceLoad(
                true, JdtPortalChunkLoadPolicy.Mode.NONE, true
        ));
    }

    @Test
    void ownerOnlineRequiresOwnerPresent() {
        assertFalse(JdtPortalChunkLoadPolicy.shouldForceLoad(
                true, JdtPortalChunkLoadPolicy.Mode.OWNER_ONLINE, false
        ));
        assertTrue(JdtPortalChunkLoadPolicy.shouldForceLoad(
                true, JdtPortalChunkLoadPolicy.Mode.OWNER_ONLINE, true
        ));
    }

    @Test
    void ownerOnlineHelper() {
        UUID owner = UUID.randomUUID();
        assertFalse(JdtPortalChunkLoadPolicy.isOwnerOnline(null, true));
        assertFalse(JdtPortalChunkLoadPolicy.isOwnerOnline(owner, false));
        assertTrue(JdtPortalChunkLoadPolicy.isOwnerOnline(owner, true));
    }
}
