package com.mawlee.cointcore.ftb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceLoadIdlePolicyTest {
    private static final long HOUR = 3_600_000L;
    private static final long NOW = 1_000L * HOUR;
    private static final long IDLE_24H = 24 * HOUR;

    private static boolean unforce(boolean server, boolean online, boolean extended, boolean unknown, long lastLogin) {
        return ForceLoadIdlePolicy.shouldUnforce(server, online, extended, unknown, lastLogin, NOW, IDLE_24H);
    }

    @Test
    void releasesPlainTeamIdlePastWindow() {
        assertTrue(unforce(false, false, false, false, NOW - 25 * HOUR));
        assertFalse(unforce(false, false, false, false, NOW - 23 * HOUR));
    }

    @Test
    void keepsExtendedOnlineUnknownAndServerTeams() {
        long longIdle = NOW - 48 * HOUR;
        assertFalse(unforce(false, false, true, false, longIdle));
        assertFalse(unforce(false, true, false, false, longIdle));
        assertFalse(unforce(false, false, false, true, longIdle));
        assertFalse(unforce(true, false, false, false, longIdle));
    }

    @Test
    void neverRecordedLoginIsNotIdle() {
        assertFalse(unforce(false, false, false, false, 0L));
    }
}
