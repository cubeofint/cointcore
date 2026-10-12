package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void ringOfSixSurgesAroundOneBeExactlyOneAccelerates() {
        int[] machine = {4, 64, 7};
        record Side(String facing, int x, int y, int z) {
        }
        Side[] ring = {
                new Side("up", 4, 65, 7),
                new Side("down", 4, 63, 7),
                new Side("north", 4, 64, 6),
                new Side("south", 4, 64, 8),
                new Side("west", 3, 64, 7),
                new Side("east", 5, 64, 7)
        };

        int accelerated = 0;
        Long owner = null;
        for (Side side : ring) {
            int[] target = SoulSurgeTarget.attachedCoordinates(side.x, side.y, side.z, side.facing);
            assertEquals(machine[0], target[0], side.facing);
            assertEquals(machine[1], target[1], side.facing);
            assertEquals(machine[2], target[2], side.facing);
            assertTrue(SoulSurgeTarget.shouldAccelerate(false, true, "minecraft:furnace"));
            assertFalse(SoulSurgeTarget.shouldAccelerate(
                    false, true, SoulSurgeTarget.SOUL_SURGE_BLOCK_ID
            ));

            long targetPacked = SoulSurgeAccelerationClaim.pack(target[0], target[1], target[2]);
            long surgePacked = SoulSurgeAccelerationClaim.pack(side.x, side.y, side.z);
            if (claims.tryClaim(targetPacked, surgePacked, 1L)) {
                accelerated++;
                owner = surgePacked;
            }
        }
        assertEquals(1, accelerated);
        assertNotNull(owner);
        assertEquals(owner, claims.peekOwner(TARGET, 1L));
    }

    @Test
    void packRoundTripIncludingNegatives() {
        assertPacked(-8, -64, 12);
        assertPacked(0, 0, 0);
        assertPacked(30000000, 319, -30000000);
    }

    @Test
    void peekOwnerReturnsStickyThenClearsAfterIdle() {
        long owner = SIX_SIDES[0];
        assertNull(claims.peekOwner(TARGET, 1L));
        assertTrue(claims.tryClaim(TARGET, owner, 1L));
        assertEquals(owner, claims.peekOwner(TARGET, 1L));
        assertEquals(owner, claims.peekOwner(TARGET, 2L));
        assertNull(claims.peekOwner(TARGET, 4L));
    }

    private static void assertPacked(int x, int y, int z) {
        long packed = SoulSurgeAccelerationClaim.pack(x, y, z);
        assertEquals(x, SoulSurgeAccelerationClaim.unpackX(packed));
        assertEquals(y, SoulSurgeAccelerationClaim.unpackY(packed));
        assertEquals(z, SoulSurgeAccelerationClaim.unpackZ(packed));
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

    @Test
    void onlySurgeWithSoulsAccelerates() {
        long dry = SIX_SIDES[0];
        long fed = SIX_SIDES[1];

        assertFalse(SoulSurgeAcceleration.canWorkThisTick(0, 0));
        assertTrue(SoulSurgeAcceleration.canWorkThisTick(0, 3));
        assertTrue(SoulSurgeAcceleration.canWorkThisTick(12, 0));
        // After IF Souls consumes the last network soul, leftover tickingTime
        // still lets that surge work / keep the claim for SOUL_TIME ticks.
        assertTrue(SoulSurgeAcceleration.canWorkThisTick(300, 0));

        assertFalse(tick(dry, false, 1L));
        assertTrue(tick(fed, true, 1L));
        assertFalse(tick(dry, false, 1L));

        assertEquals(fed, claims.peekOwner(TARGET, 1L));
        assertTrue(claims.isOwnerActive(TARGET, fed, 1L));
        assertFalse(claims.isOwnerActive(TARGET, dry, 1L));
    }

    @Test
    void ownerLosesSoulsOtherTakesOverSameTick() {
        long owner = SIX_SIDES[2];
        long other = SIX_SIDES[3];
        assertTrue(tick(owner, true, 5L));
        assertFalse(tick(other, true, 5L));

        assertFalse(tick(owner, false, 6L));
        assertTrue(tick(other, true, 6L));
        assertFalse(tick(owner, false, 6L));

        assertEquals(other, claims.peekOwner(TARGET, 6L));
        assertTrue(claims.isOwnerActive(TARGET, other, 6L));
        assertEquals(1, acceleratedThisTick(6L, owner, other));
    }

    @Test
    void stillAtMostOneAcceleratesPerTick() {
        assertEquals(1, acceleratedThisTick(7L, SIX_SIDES[0], SIX_SIDES[1], SIX_SIDES[2]));
        assertTrue(claims.isOwnerActive(TARGET, SIX_SIDES[0], 7L));
        assertFalse(claims.isOwnerActive(TARGET, SIX_SIDES[1], 7L));
    }

    @Test
    void peekCurrentOwnerIsEmptyWhileOnlySticky() {
        long owner = SIX_SIDES[4];
        assertTrue(claims.tryClaim(TARGET, owner, 1L));
        assertEquals(owner, claims.peekCurrentOwner(TARGET, 1L));
        assertNull(claims.peekCurrentOwner(TARGET, 2L));
        assertEquals(owner, claims.peekOwner(TARGET, 2L));
        assertFalse(claims.isOwnerActive(TARGET, owner, 2L));
    }

    private boolean tick(long surge, boolean hasSouls, long gameTime) {
        return SoulSurgeAcceleration.tryAccelerate(claims, TARGET, surge, gameTime, hasSouls);
    }

    private int acceleratedThisTick(long gameTime, long... surges) {
        int accelerated = 0;
        for (long surge : surges) {
            if (tick(surge, true, gameTime)) {
                accelerated++;
            }
        }
        return accelerated;
    }
}
