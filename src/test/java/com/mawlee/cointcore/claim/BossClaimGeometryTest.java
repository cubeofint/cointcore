package com.mawlee.cointcore.claim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossClaimGeometryTest {
    @Test
    void chunkContainingSpikeCenterIsProtected() {
        assertTrue(BossClaimGeometry.chunkIntersectsDisk(0, 0, 8, 8, 3));
    }

    @Test
    void distantChunkIsNotProtectedBySpike() {
        assertFalse(BossClaimGeometry.chunkIntersectsDisk(5, 5, 8, 8, 3));
    }

    @Test
    void spikeOnChunkEdgeStillIntersects() {
        assertTrue(BossClaimGeometry.chunkIntersectsDisk(1, 0, 15, 8, 1));
    }

    @Test
    void portalPadUsesChebyshevRadius() {
        assertTrue(BossClaimGeometry.chunkWithinChebyshev(2, 1, 0, 0, 2));
        assertFalse(BossClaimGeometry.chunkWithinChebyshev(3, 0, 0, 0, 2));
    }
}
