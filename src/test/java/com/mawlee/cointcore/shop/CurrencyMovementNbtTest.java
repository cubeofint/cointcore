package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyMovementNbtTest {
    private static final UUID FROM = UUID.fromString("f3fc162d-d344-32fa-8c9a-0b987b0791cf");
    private static final UUID TO = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void roundTripsLegacyRecordWithoutDeltas() {
        Map<String, Object> old = new LinkedHashMap<>();
        old.put("id", 812L);
        old.put("timestamp", 1_791_400_000_000L);
        old.put("from_id", FROM.toString());
        old.put("from_name", "Nick");
        old.put("to_name", "trader");
        old.put("amount", 40L);
        old.put("type", "trader_buy");
        old.put("note", "offer=diamond");

        CurrencyMovement loaded = CurrencyMovement.loadMap(old);
        assertEquals(812L, loaded.id());
        assertEquals(FROM, loaded.fromId());
        assertTrue(loaded.deltas().isEmpty());
        assertNull(loaded.siteOpId());

        Map<String, Object> saved = loaded.saveMap();
        assertEquals(40L, saved.get("amount"));
        assertEquals("trader_buy", saved.get("type"));
        assertNull(saved.get("deltas"));
        assertNull(saved.get("site_op_id"));
        assertEquals(loaded, CurrencyMovement.loadMap(saved));
    }

    @Test
    void roundTripsDeltasAndSiteOpId() {
        CurrencyMovement movement = new CurrencyMovement(
                9L,
                100L,
                FROM,
                "Nick",
                TO,
                "Bob",
                15L,
                CurrencyMovementType.PAY,
                "tip",
                List.of(
                        new CurrencyMovement.Delta(FROM, -15L, 910L),
                        new CurrencyMovement.Delta(TO, 15L, 40L)
                ),
                "op_12345678"
        );
        CurrencyMovement loaded = CurrencyMovement.loadMap(movement.saveMap());
        assertEquals(movement, loaded);
        assertEquals(2, loaded.deltas().size());
        assertEquals(-15L, loaded.deltas().get(0).delta());
        assertEquals(910L, loaded.deltas().get(0).balanceAfter());
        assertEquals("op_12345678", loaded.siteOpId());
    }
}
