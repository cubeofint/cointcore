package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteOperationApplyTest {
    @Test
    void creditsToServerAndAdjust() {
        SiteOperationApply.Decision toServer = SiteOperationApply.decide(SiteOperation.Kind.TO_SERVER, 40, 100);
        assertEquals("applied", toServer.status());
        assertEquals(140L, toServer.balanceAfter());
        assertEquals(40L, toServer.signedDelta());
        assertTrue(toServer.mutated());

        SiteOperationApply.Decision adjust = SiteOperationApply.decide(SiteOperation.Kind.ADJUST, 25, 10);
        assertEquals("applied", adjust.status());
        assertEquals(35L, adjust.balanceAfter());
        assertEquals(25L, adjust.signedDelta());
    }

    @Test
    void debitsFromServerAndAdjust() {
        SiteOperationApply.Decision fromServer = SiteOperationApply.decide(SiteOperation.Kind.FROM_SERVER, 30, 90);
        assertEquals("applied", fromServer.status());
        assertEquals(60L, fromServer.balanceAfter());
        assertEquals(-30L, fromServer.signedDelta());

        SiteOperationApply.Decision adjust = SiteOperationApply.decide(SiteOperation.Kind.ADJUST, -50, 80);
        assertEquals("applied", adjust.status());
        assertEquals(30L, adjust.balanceAfter());
        assertEquals(-50L, adjust.signedDelta());
    }

    @Test
    void insufficientDebitDoesNotChangeBalance() {
        SiteOperationApply.Decision decision = SiteOperationApply.decide(SiteOperation.Kind.ADJUST, -50, 30);
        assertEquals("failed", decision.status());
        assertEquals(SiteOperationApply.INSUFFICIENT_SERVER_BALANCE, decision.error());
        assertEquals(30L, decision.balanceAfter());
        assertFalse(decision.mutated());
        assertEquals(0L, decision.signedDelta());
    }

    @Test
    void zeroAdjustProbeLeavesBalance() {
        SiteOperationApply.Decision decision = SiteOperationApply.decide(SiteOperation.Kind.ADJUST, 0, 950);
        assertEquals("applied", decision.status());
        assertNull(decision.error());
        assertEquals(950L, decision.balanceAfter());
        assertFalse(decision.mutated());
    }

    @Test
    void duplicateReplayKeepsStoredStatusAndBalance() {
        SiteOperationApply.Decision first = SiteOperationApply.decide(SiteOperation.Kind.ADJUST, -20, 50);
        SiteOperationApply.Decision replay = SiteOperationApply.replay(first.status(), first.balanceAfter(), 999);
        assertEquals("applied", replay.status());
        assertEquals(999L, replay.balanceAfter());
        assertFalse(replay.mutated());

        SiteOperationApply.Decision failed = SiteOperationApply.decide(SiteOperation.Kind.FROM_SERVER, 10, 3);
        SiteOperationApply.Decision failedAgain = SiteOperationApply.replay(failed.status(), failed.balanceAfter(), 3);
        assertEquals("failed", failedAgain.status());
        assertEquals(SiteOperationApply.INSUFFICIENT_SERVER_BALANCE, failedAgain.error());
        assertEquals(3L, failedAgain.balanceAfter());
    }
}
