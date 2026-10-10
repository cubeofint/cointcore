package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalMarketRowsTest {
    @Test
    void singleSellerKeepsNicknameAndCheapestId() {
        List<GlobalMarketRows.Row> rows = GlobalMarketRows.group(List.of(
                source("a", "bone|3", 3, 248L, 2, "Mawlee", 10L)
        ), false);
        assertEquals(1, rows.size());
        assertEquals("Mawlee", rows.getFirst().cheapestSeller());
        assertFalse(rows.getFirst().multiSeller());
        assertEquals(2, rows.getFirst().dealsLeft());
        assertEquals(1, rows.getFirst().listingCount());
        assertEquals(82L, rows.getFirst().unitPrice());
    }

    @Test
    void groupsIdenticalLotsAndListsSellersFromCheapest() {
        List<GlobalMarketRows.Row> rows = GlobalMarketRows.group(List.of(
                source("dear", "bone|3", 3, 400L, 1, "Bob", 5L),
                source("cheap", "bone|3", 3, 248L, 2, "Mawlee", 8L),
                source("mid", "bone|3", 3, 300L, 1, "Ann", 20L)
        ), false);
        assertEquals(1, rows.size());
        GlobalMarketRows.Row row = rows.getFirst();
        assertEquals("cheap", row.listingId());
        assertEquals(248L, row.price());
        assertEquals(2, row.dealsLeft());
        assertEquals(3, row.listingCount());
        assertTrue(row.multiSeller());
        assertEquals("Mawlee", row.cheapestSeller());
        assertEquals(List.of("Mawlee", "Ann", "Bob"), row.sellers());
        assertEquals(3, row.offers().size());
        assertEquals("cheap", row.offers().getFirst().listingId());
        assertEquals("Mawlee", row.offers().getFirst().sellerName());
        assertEquals(82L, row.offers().getFirst().unitPrice());
        assertEquals(6L, row.offers().getFirst().stockItems());
        assertEquals(3, row.offers().getFirst().countPerDeal());
        assertEquals("mid", row.offers().get(1).listingId());
        assertEquals("dear", row.offers().get(2).listingId());
        assertEquals("mid", row.offer("mid").listingId());
    }

    @Test
    void differentCountsStaySeparate() {
        List<GlobalMarketRows.Row> rows = GlobalMarketRows.group(List.of(
                source("x", "bone|1", 1, 80L, 1, "A", 1L),
                source("y", "bone|3", 3, 240L, 1, "B", 1L)
        ), false);
        assertEquals(2, rows.size());
    }

    private static GlobalMarketRows.Source source(
            String id,
            String key,
            int count,
            long price,
            int deals,
            String seller,
            long created
    ) {
        return new GlobalMarketRows.Source(id, key, count, price, deals, seller, created, created + 1, 80L, "minecraft");
    }
}
