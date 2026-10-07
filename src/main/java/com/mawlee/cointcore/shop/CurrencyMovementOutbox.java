package com.mawlee.cointcore.shop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory outbox for server-wallet currency movements. Survives restarts via {@link CurrencyMovementSavedData}.
 * Only {@link CurrencyMovementType#isServerWalletLog()} rows are offered to the site; site&lt;-&gt;server
 * transfers stay local so they are not echoed back as new site-wallet mutations.
 */
final class CurrencyMovementOutbox {
    static final int MAX_ENTRIES = 10_000;

    private final Deque<CurrencyMovement> entries = new ArrayDeque<>();
    private long nextId = 1L;
    private long siteSentUpTo;

    CurrencyMovement append(CurrencyMovement draft) {
        CurrencyMovement stored = new CurrencyMovement(
                nextId++,
                draft.timestampMs(),
                draft.fromId(),
                draft.fromName(),
                draft.toId(),
                draft.toName(),
                draft.amount(),
                draft.type(),
                draft.note(),
                draft.deltas(),
                draft.siteOpId()
        );
        entries.addLast(stored);
        trimSentOverflow();
        return stored;
    }

    List<CurrencyMovement> snapshot() {
        return new ArrayList<>(entries);
    }

    long nextId() {
        return nextId;
    }

    long siteSentUpTo() {
        return siteSentUpTo;
    }

    void restore(long nextId, long siteSentUpTo, List<CurrencyMovement> loaded) {
        this.nextId = Math.max(1L, nextId);
        this.siteSentUpTo = Math.max(0L, siteSentUpTo);
        entries.clear();
        entries.addAll(loaded);
        trimSentOverflow();
    }

    boolean markSiteSentUpTo(long id) {
        if (id <= siteSentUpTo) {
            return false;
        }
        siteSentUpTo = id;
        trimSentOverflow();
        return true;
    }

    /** Oldest server-wallet movements the site has not confirmed. */
    List<CurrencyMovement> unsentForSite(int limit) {
        int cap = Math.max(1, limit);
        List<CurrencyMovement> result = new ArrayList<>();
        for (CurrencyMovement entry : entries) {
            if (entry.id() <= siteSentUpTo || !entry.type().isServerWalletLog()) {
                continue;
            }
            result.add(entry);
            if (result.size() >= cap) {
                break;
            }
        }
        return result;
    }

    /**
     * Drop only already-confirmed rows when over the cap. Unsent movements are kept so a long
     * outage cannot silently lose the outbox.
     */
    private void trimSentOverflow() {
        while (entries.size() > MAX_ENTRIES) {
            CurrencyMovement head = entries.peekFirst();
            if (head == null || head.id() > siteSentUpTo) {
                break;
            }
            entries.removeFirst();
        }
    }
}
