package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MarketHistoryTest {
    @Test
    void filtersBothSidesOfTradesAndExcludesUnrelatedWalletOperations() {
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        var rows = List.of(row(1, player, other, CurrencyMovementType.TRADER_BUY),
                row(2, other, player, CurrencyMovementType.PLAYER_SHOP_SELL),
                row(3, other, other, CurrencyMovementType.PLAYER_SHOP_BUY),
                row(4, player, other, CurrencyMovementType.PAY),
                row(5, null, player, CurrencyMovementType.ADMIN_ADD));
        assertEquals(List.of(2L, 1L), MarketHistory.page(rows, player, 1).stream().map(CurrencyMovement::id).toList());
        assertEquals(3, MarketHistory.page(rows, null, 1).size());
        assertEquals(5, rows.size());
    }

    @Test
    void paginatesNewestFirstAndHandlesLargePageNumbersWithoutOverflow() {
        var rows = new ArrayList<CurrencyMovement>();
        for (long id = 1; id <= 23; id++) rows.add(row(id, null, null, CurrencyMovementType.TRADER_SELL));
        assertEquals(23L, MarketHistory.page(rows, null, 1).getFirst().id());
        assertEquals(13L, MarketHistory.page(rows, null, 2).getFirst().id());
        assertEquals(List.of(3L, 2L, 1L), MarketHistory.page(rows, null, 3).stream().map(CurrencyMovement::id).toList());
        assertTrue(MarketHistory.page(rows, null, Integer.MAX_VALUE).isEmpty());
        assertTrue(MarketHistory.page(rows, null, 0).isEmpty());
    }

    private static CurrencyMovement row(long id, UUID from, UUID to, CurrencyMovementType type) {
        return new CurrencyMovement(id, id, from, "from", to, "to", 10L, type, "note");
    }
}
