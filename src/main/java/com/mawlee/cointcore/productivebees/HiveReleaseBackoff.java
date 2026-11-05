package com.mawlee.cointcore.productivebees;

/**
 * Productive Bees advanced hives try to release every ready bee each tick. Each try loads the bee entity from NBT,
 * plays the exit sound and posts events before {@code addFreshEntity}; when the spawn is refused (a chunk mob limit,
 * for one) the bee stays inside and the hive tries again next tick, forever. After a failed release the hive waits
 * {@link #RETRY_TICKS} before trying again.
 */
public interface HiveReleaseBackoff {
    /** Five seconds: bees still come out promptly once there is room, at 1% of the cost. */
    long RETRY_TICKS = 100L;

    long cointcore$releaseRetryAt();

    void cointcore$setReleaseRetryAt(long gameTime);

    /** True while the hive is still waiting after a failed release. */
    static boolean waiting(HiveReleaseBackoff hive, long gameTime) {
        return gameTime < hive.cointcore$releaseRetryAt();
    }

    static void recordResult(HiveReleaseBackoff hive, long gameTime, boolean released) {
        hive.cointcore$setReleaseRetryAt(released ? 0L : gameTime + RETRY_TICKS);
    }
}
