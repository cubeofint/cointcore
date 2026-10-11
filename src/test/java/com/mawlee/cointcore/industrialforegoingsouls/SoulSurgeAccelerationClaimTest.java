package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeAccelerationClaimTest {
    private static final long TARGET = SoulSurgeAccelerationClaim.pack(4, 64, 7);
    private static final long[] SIX_SIDES = {
            SoulSurgeAccelerationClaim.pack(4, 65, 7),
            SoulSurgeAccelerationClaim.pack(4, 63, 7),
            SoulSurgeAccelerationClaim.pack(4, 64, 8),
            SoulSurgeAccelerationClaim.pack(4, 64, 6),
            SoulSurgeAccelerationClaim.pack(5, 64, 7),
            SoulSurgeAccelerationClaim.pack(3, 64, 7)
    };

    private SoulSurgeAccelerationClaim claims;

    @BeforeEach
    void newMap() {
        claims = new SoulSurgeAccelerationClaim();
    }

    @Test
    void sixSurgesOneTargetOnlyFirstAccelerates() {
        assertTrue(claims.tryClaim(TARGET, SIX_SIDES[0], 10L));
        for (int i = 1; i < SIX_SIDES.length; i++) {
            assertFalse(
                    claims.tryClaim(TARGET, SIX_SIDES[i], 10L),
                    "side " + i + " must skip and not consume souls"
            );
        }
        assertTrue(claims.tryClaim(TARGET, SIX_SIDES[0], 10L));
    }

    @Test
    void stickyOwnerKeepsClaimWhenOthersTickFirst() {
        long owner = SIX_SIDES[2];
        long other = SIX_SIDES[5];
        assertTrue(claims.tryClaim(TARGET, owner, 1L));
        assertFalse(claims.tryClaim(TARGET, other, 1L));

        assertFalse(claims.tryClaim(TARGET, other, 2L));
        assertTrue(claims.tryClaim(TARGET, owner, 2L));
        assertFalse(claims.tryClaim(TARGET, other, 2L));

        assertFalse(claims.tryClaim(TARGET, other, 3L));
        assertTrue(claims.tryClaim(TARGET, owner, 3L));
    }

    @Test
    void ownerRemovedReleasesClaimSameTick() {
        long owner = SIX_SIDES[0];
        long other = SIX_SIDES[1];
        assertTrue(claims.tryClaim(TARGET, owner, 4L));

        claims.release(TARGET, owner, 4L);

        assertTrue(claims.tryClaim(TARGET, other, 4L));
        assertFalse(claims.tryClaim(TARGET, owner, 4L));
    }

    @Test
    void ownerStoppedWithoutReleaseYieldsAfterOneIdleTick() {
        long owner = SIX_SIDES[3];
        long other = SIX_SIDES[4];
        assertTrue(claims.tryClaim(TARGET, owner, 8L));

        assertFalse(claims.tryClaim(TARGET, other, 9L));
        assertTrue(claims.tryClaim(TARGET, other, 10L));
        assertFalse(claims.tryClaim(TARGET, owner, 10L));
    }

    @Test
    void releaseOfNonOwnerIsIgnored() {
        long owner = SIX_SIDES[0];
        long other = SIX_SIDES[1];
        assertTrue(claims.tryClaim(TARGET, owner, 1L));
        claims.release(TARGET, other, 1L);
        assertTrue(claims.tryClaim(TARGET, owner, 1L));
        assertFalse(claims.tryClaim(TARGET, other, 1L));
    }

    @Test
    void differentTargetsAreIndependent() {
        long otherTarget = SoulSurgeAccelerationClaim.pack(0, 0, 0);
        assertTrue(claims.tryClaim(TARGET, SIX_SIDES[0], 1L));
        assertTrue(claims.tryClaim(otherTarget, SIX_SIDES[1], 1L));
    }
}
