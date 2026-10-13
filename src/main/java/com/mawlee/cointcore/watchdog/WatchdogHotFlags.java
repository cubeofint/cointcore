package com.mawlee.cointcore.watchdog;

/**
 * Volatile category switches read by mixins on the hottest tick paths.
 * Disabled categories cost one boolean load and an early return.
 */
public final class WatchdogHotFlags {
    public static volatile boolean RUNNING;
    public static volatile boolean TIME_BLOCK_ENTITIES;
    public static volatile boolean TIME_ENTITIES;
    public static volatile boolean PROBE_PATHFINDING;
    public static volatile boolean PROBE_BLOCK_UPDATES;
    public static volatile boolean PROBE_DISK_SAVE;
    public static volatile boolean PROBE_CHUNKS;

    private WatchdogHotFlags() {
    }

    public static void setTiming(boolean blockEntities, boolean entities) {
        TIME_BLOCK_ENTITIES = blockEntities;
        TIME_ENTITIES = entities;
    }

    public static void setProbes(
            boolean active,
            boolean pathfinding,
            boolean blockUpdates,
            boolean diskSave,
            boolean chunks
    ) {
        PROBE_PATHFINDING = active && pathfinding;
        PROBE_BLOCK_UPDATES = active && blockUpdates;
        PROBE_DISK_SAVE = active && diskSave;
        PROBE_CHUNKS = active && chunks;
    }

    public static void clear() {
        RUNNING = false;
        TIME_BLOCK_ENTITIES = false;
        TIME_ENTITIES = false;
        PROBE_PATHFINDING = false;
        PROBE_BLOCK_UPDATES = false;
        PROBE_DISK_SAVE = false;
        PROBE_CHUNKS = false;
    }
}
