package com.mawlee.cointcore.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderOffersConfigTest {
    @Test
    void defaultFileHasCommissionAndExampleOffers() {
        JsonObject json = TraderOffersConfig.defaultJson();
        assertEquals(2.5d, json.get("commission_percent").getAsDouble(), 0.0001d);
        JsonArray offers = json.getAsJsonArray("offers");
        assertTrue(offers.size() >= 3);
        JsonObject first = offers.get(0).getAsJsonObject();
        assertTrue(first.has("item"));
        assertTrue(first.has("buy_price"));
        assertTrue(first.has("sell_price"));
        assertTrue(first.get("enabled").getAsBoolean());
    }
}
