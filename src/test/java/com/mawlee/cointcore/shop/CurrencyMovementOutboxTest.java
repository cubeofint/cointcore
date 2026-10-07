package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyMovementOutboxTest {
    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void assignsStableIdsAndKeepsUnsentAcrossMarks() {
        CurrencyMovementOutbox outbox = new CurrencyMovementOutbox();
        CurrencyMovement first = outbox.append(draft(CurrencyMovementType.PAY, 10));
        CurrencyMovement second = outbox.append(draft(CurrencyMovementType.TRADER_BUY, 3));
        assertEquals(1L, first.id());
        assertEquals(2L, second.id());
        assertEquals(List.of(first, second), outbox.unsentForSite(10));

        assertTrue(outbox.markSiteSentUpTo(1L));
        assertEquals(1L, outbox.siteSentUpTo());
        assertEquals(List.of(second), outbox.unsentForSite(10));
        outbox.markSiteSentUpTo(1L);
        assertEquals(1L, outbox.siteSentUpTo());
    }

    @Test
    void skipsSiteQueueTypesAndRespectsBatchLimit() {
        CurrencyMovementOutbox outbox = new CurrencyMovementOutbox();
        outbox.append(draft(CurrencyMovementType.PAY, 1));
        outbox.append(draft(CurrencyMovementType.SITE_TO_SERVER, 9));
        outbox.append(draft(CurrencyMovementType.ADMIN_ADD, 2));
        outbox.append(draft(CurrencyMovementType.SERVER_TO_SITE, 4));
        outbox.append(draft(CurrencyMovementType.TRADER_SELL, 5));

        List<CurrencyMovement> batch = outbox.unsentForSite(2);
        assertEquals(2, batch.size());
        assertEquals(CurrencyMovementType.PAY, batch.get(0).type());
        assertEquals(CurrencyMovementType.ADMIN_ADD, batch.get(1).type());
        assertEquals(1L, batch.get(0).id());
        assertEquals(3L, batch.get(1).id());
    }

    @Test
    void restoreReplaysCursorAndDoesNotDropUnsentOnOverflow() {
        CurrencyMovementOutbox outbox = new CurrencyMovementOutbox();
        List<CurrencyMovement> loaded = List.of(
                new CurrencyMovement(1L, 1L, A, "a", B, "b", 1L, CurrencyMovementType.PAY, null),
                new CurrencyMovement(2L, 2L, A, "a", B, "b", 1L, CurrencyMovementType.PAY, null)
        );
        outbox.restore(3L, 1L, loaded);
        assertEquals(1L, outbox.siteSentUpTo());
        assertEquals(3L, outbox.nextId());
        assertEquals(1, outbox.unsentForSite(10).size());
        assertEquals(2L, outbox.unsentForSite(10).getFirst().id());
    }

    private static CurrencyMovement draft(CurrencyMovementType type, long amount) {
        return new CurrencyMovement(0L, 1L, A, "from", B, "to", amount, type, "n");
    }
}
