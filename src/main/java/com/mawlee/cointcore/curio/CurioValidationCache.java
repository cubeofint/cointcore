package com.mawlee.cointcore.curio;

import java.util.WeakHashMap;

/** A metadata-only update cannot add another copy; configuration changes still force validation. */
public final class CurioValidationCache<P> {
    private final WeakHashMap<P, Long> checked = new WeakHashMap<>();

    public boolean needsValidation(P player, long revision, boolean sameItem, int oldCount, int newCount) {
        var previous = checked.get(player);
        return !sameItem || oldCount != newCount || previous == null || previous.longValue() != revision;
    }

    public void validated(P player, long revision) {
        checked.put(player, revision);
    }
}
