package com.mawlee.cointcore.chunklimit;

/**
 * Tracks whether the current thread is inside NeoForge {@code CommonHooks.onPlaceItemIntoWorld}.
 * That hook restores the used stack itself when a place event is cancelled, so a denied placement
 * there must not hand out another copy of the block.
 */
public final class ItemPlacementGuard {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private ItemPlacementGuard() {
    }

    public static void enter() {
        DEPTH.get()[0]++;
    }

    public static void exit() {
        int[] depth = DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean isPlacingFromItem() {
        return DEPTH.get()[0] > 0;
    }
}
