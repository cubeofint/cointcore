package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteMovementPayloadTest {
    @Test
    void backoffDoublesUpToCap() {
        assertEquals(5_000L, SiteMovementPayload.nextBackoff(0));
        assertEquals(10_000L, SiteMovementPayload.nextBackoff(5_000L));
        assertEquals(300_000L, SiteMovementPayload.nextBackoff(200_000L));
        assertEquals(300_000L, SiteMovementPayload.nextBackoff(300_000L));
    }

    @Test
    void acceptedUpToRequiresSiteConfirmation() {
        assertEquals(7L, SiteMovementPayload.acceptedUpTo("{\"accepted_up_to\":7,\"stored\":7}", 9L));
        assertEquals(9L, SiteMovementPayload.acceptedUpTo("{\"accepted_up_to\":99}", 9L));
        assertEquals(null, SiteMovementPayload.acceptedUpTo("garbage", 9L));
        assertEquals(null, SiteMovementPayload.acceptedUpTo("{\"stored\":9}", 9L));
        assertEquals(null, SiteMovementPayload.acceptedUpTo("{\"accepted_up_to\":0}", 9L));
        assertEquals(null, SiteMovementPayload.acceptedUpTo(null, 9L));
    }

    @Test
    void toJsonEmitsDeltasAndSiteOpId() {
        java.util.UUID uuid = java.util.UUID.fromString("f3fc162d-d344-32fa-8c9a-0b987b0791cf");
        CurrencyMovement movement = new CurrencyMovement(
                812L,
                1_791_400_000_000L,
                uuid,
                "Nick",
                null,
                "trader",
                40L,
                CurrencyMovementType.TRADER_BUY,
                "offer=diamond",
                java.util.List.of(new CurrencyMovement.Delta(uuid, -40L, 910L)),
                null
        );
        String json = SiteMovementPayload.toJson(java.util.List.of(movement));
        com.google.gson.JsonObject row = com.google.gson.JsonParser.parseString(json)
                .getAsJsonObject().getAsJsonArray("movements").get(0).getAsJsonObject();
        assertEquals("trader_buy", row.get("type").getAsString());
        assertEquals(-40L, row.getAsJsonArray("deltas").get(0).getAsJsonObject().get("delta").getAsLong());
        assertEquals(910L, row.getAsJsonArray("deltas").get(0).getAsJsonObject().get("balance_after").getAsLong());
        assertEquals(uuid.toString(), row.getAsJsonArray("deltas").get(0).getAsJsonObject().get("uuid").getAsString());
        assertTrue(row.get("site_op_id").isJsonNull());
    }
}
