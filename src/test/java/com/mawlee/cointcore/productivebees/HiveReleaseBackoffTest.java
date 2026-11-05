package com.mawlee.cointcore.productivebees;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HiveReleaseBackoffTest {
    private static final class Hive implements HiveReleaseBackoff {
        long retryAt;

        @Override
        public long cointcore$releaseRetryAt() {
            return retryAt;
        }

        @Override
        public void cointcore$setReleaseRetryAt(long gameTime) {
            retryAt = gameTime;
        }
    }

    @Test
    void aRefusedReleaseWaitsBeforeTheNextTry() {
        Hive hive = new Hive();
        assertFalse(HiveReleaseBackoff.waiting(hive, 1000));
        HiveReleaseBackoff.recordResult(hive, 1000, false);
        assertTrue(HiveReleaseBackoff.waiting(hive, 1001));
        assertTrue(HiveReleaseBackoff.waiting(hive, 1000 + HiveReleaseBackoff.RETRY_TICKS - 1));
        assertFalse(HiveReleaseBackoff.waiting(hive, 1000 + HiveReleaseBackoff.RETRY_TICKS));
    }

    @Test
    void aSuccessfulReleaseKeepsTheHiveReady() {
        Hive hive = new Hive();
        HiveReleaseBackoff.recordResult(hive, 1000, false);
        HiveReleaseBackoff.recordResult(hive, 1100, true);
        assertFalse(HiveReleaseBackoff.waiting(hive, 1100));
    }
}
