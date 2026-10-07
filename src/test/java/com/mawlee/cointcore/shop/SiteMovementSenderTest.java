package com.mawlee.cointcore.shop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteMovementSenderTest {
    @Test
    void serializesBatchWithoutSiteBalanceFields() {
        UUID from = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        CurrencyMovement pay = new CurrencyMovement(
                12L, 1_700_000_000_000L, from, "Alice", null, null, 25L, CurrencyMovementType.PAY, "tip",
                List.of(new CurrencyMovement.Delta(from, -25L, 10L)), null);
        String json = SiteMovementPayload.toJson(List.of(pay));
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject row = root.getAsJsonArray("movements").get(0).getAsJsonObject();
        assertEquals(12L, row.get("id").getAsLong());
        assertEquals("pay", row.get("type").getAsString());
        assertEquals(25L, row.get("amount").getAsLong());
        assertEquals(from.toString(), row.get("from_id").getAsString());
        assertTrue(row.get("to_id").isJsonNull());
        assertEquals("tip", row.get("note").getAsString());
        assertEquals(-25L, row.getAsJsonArray("deltas").get(0).getAsJsonObject().get("delta").getAsLong());
        assertTrue(row.get("site_op_id").isJsonNull());
        assertTrue(!json.contains("set_balance") && !json.contains("absolute"));
    }
}
