package com.mawlee.cointcore.shop;

/**
 * Wipe-safe movement id arithmetic. {@code next_id} must never go backwards after the site
 * has accepted ids, even if overworld SavedData is deleted.
 */
final class CurrencyMovementSequence {
    private CurrencyMovementSequence() {
    }

    record Snapshot(long nextId, long acceptedUpTo) {
    }

    static Snapshot reconcile(long localNextId, long acceptedUpTo) {
        long accepted = Math.max(0L, acceptedUpTo);
        long next = Math.max(Math.max(1L, localNextId), accepted + 1L);
        return new Snapshot(next, accepted);
    }

    static Snapshot applyAccepted(Snapshot current, long siteAcceptedUpTo) {
        Snapshot base = current != null ? current : new Snapshot(1L, 0L);
        return reconcile(base.nextId(), Math.max(base.acceptedUpTo(), siteAcceptedUpTo));
    }

    static Snapshot applyNextId(Snapshot current, long nextId) {
        Snapshot base = current != null ? current : new Snapshot(1L, 0L);
        return reconcile(Math.max(base.nextId(), nextId), base.acceptedUpTo());
    }

    static Snapshot merge(long worldNextId, long fileNextId, long worldAccepted, long fileAccepted) {
        return reconcile(
                Math.max(worldNextId, fileNextId),
                Math.max(worldAccepted, fileAccepted)
        );
    }
}
