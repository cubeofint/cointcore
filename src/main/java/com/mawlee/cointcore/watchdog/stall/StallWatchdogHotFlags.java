package com.mawlee.cointcore.watchdog.stall;

/**
 * Volatile flags read on the server-thread hot path. Written by the stall daemon.
 */
public final class StallWatchdogHotFlags {
    public static volatile boolean ENABLED;
    public static volatile boolean ABORT_DAMAGE;
    public static volatile boolean ABORT_ENTITY;
    public static volatile int ABORT_ENTITY_ID;
    public static volatile boolean DISCARD_ENTITY;
    public static volatile boolean TEST_STALL;

    private StallWatchdogHotFlags() {
    }

    public static void clearAbort() {
        ABORT_DAMAGE = false;
        ABORT_ENTITY = false;
        ABORT_ENTITY_ID = 0;
        DISCARD_ENTITY = false;
    }
}
