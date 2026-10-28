package com.mawlee.cointcore.modularbees;

/** Position-based phases spread a farm's transfers across ticks; no saved counters or world scans. */
public final class HatchTransferSchedule {
    private HatchTransferSchedule() { }

    public static boolean isDue(long tick, long position, int interval) {
        int bounded = Math.max(1, interval);
        long phase = Math.floorMod(Long.hashCode(position), bounded);
        return Math.floorMod(tick, bounded) == phase;
    }
}
