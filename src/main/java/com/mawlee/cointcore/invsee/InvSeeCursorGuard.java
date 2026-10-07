package com.mawlee.cointcore.invsee;

/**
 * Tab switches close then reopen a menu in the same client tick; keep the saved
 * cursor only across that pair, not the next unrelated InvSee open.
 */
public final class InvSeeCursorGuard {
    public static final int MAX_TICK_AGE = 1;

    private InvSeeCursorGuard() {
    }

    public static boolean shouldRestore(boolean saved, int savedTick, int nowTick) {
        if (!saved) {
            return false;
        }
        return nowTick - savedTick <= MAX_TICK_AGE;
    }
}
