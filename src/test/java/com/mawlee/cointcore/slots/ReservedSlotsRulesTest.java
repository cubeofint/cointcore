package com.mawlee.cointcore.slots;

import com.mawlee.cointcore.config.ReservedSlotsParse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservedSlotsRulesTest {
    @Test
    void publicPlayersFitUntilPublicCap() {
        assertEquals(
                ReservedSlotsRules.Decision.ALLOW,
                ReservedSlotsRules.decide(29, 30, 10, false, false)
        );
        assertEquals(
                ReservedSlotsRules.Decision.PUBLIC_FULL,
                ReservedSlotsRules.decide(30, 30, 10, false, false)
        );
    }

    @Test
    void reservedPlayersUseExtraSlots() {
        assertEquals(
                ReservedSlotsRules.Decision.ALLOW,
                ReservedSlotsRules.decide(30, 30, 10, true, false)
        );
        assertEquals(
                ReservedSlotsRules.Decision.ALLOW,
                ReservedSlotsRules.decide(39, 30, 10, true, false)
        );
        assertEquals(
                ReservedSlotsRules.Decision.TOTAL_FULL,
                ReservedSlotsRules.decide(40, 30, 10, true, false)
        );
    }

    @Test
    void bypassJoinsWhenEverythingIsFull() {
        assertEquals(
                ReservedSlotsRules.Decision.ALLOW,
                ReservedSlotsRules.decide(40, 30, 10, false, true)
        );
        assertEquals(40, ReservedSlotsRules.totalSlots(30, 10));
    }

    @Test
    void parseDefaultsAndClamps() {
        ReservedSlotsParse.Parsed defaults = ReservedSlotsParse.parseJson("{}");
        assertTrue(defaults.enabled());
        assertEquals(30, defaults.publicSlots());
        assertEquals(10, defaults.reservedSlots());
        assertEquals(40, defaults.totalSlots());

        ReservedSlotsParse.Parsed custom = ReservedSlotsParse.parseJson("""
                {
                  "enabled": false,
                  "publicSlots": 20,
                  "reservedSlots": 5
                }
                """);
        assertFalse(custom.enabled());
        assertEquals(20, custom.publicSlots());
        assertEquals(5, custom.reservedSlots());

        ReservedSlotsParse.Parsed clamped = ReservedSlotsParse.parseJson("""
                {
                  "publicSlots": -1,
                  "reservedSlots": -4
                }
                """);
        assertEquals(0, clamped.publicSlots());
        assertEquals(0, clamped.reservedSlots());
    }
}
