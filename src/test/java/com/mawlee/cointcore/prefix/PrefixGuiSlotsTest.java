package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixGuiSlotsTest {
    @Test
    void autoSlotIsCopyOfSafeAndDistinctFromGroups() {
        List<String> slots = PrefixGuiSlots.of(List.of("explorer", "titan"));
        assertEquals(List.of("", "explorer", "titan"), slots);
        assertEquals("", slots.get(0));
        assertTrue(PrefixGuiSlots.isAuto(slots.get(0)));
        assertFalse(PrefixGuiSlots.isAuto(slots.get(1)));
        List.copyOf(new ArrayList<>(slots));
    }

    @Test
    void skipsNullBlankAndCapsAtChestSize() {
        List<String> many = new ArrayList<>();
        many.add(null);
        many.add(" ");
        for (int index = 0; index < 40; index++) {
            many.add("g" + index);
        }
        List<String> slots = PrefixGuiSlots.of(many);
        assertEquals(PrefixGuiSlots.MENU_SIZE, slots.size());
        assertEquals(PrefixGuiSlots.AUTO, slots.get(0));
        assertEquals("g0", slots.get(1));
        assertEquals("g25", slots.get(26));
    }

    @Test
    void isAutoTreatsNullAsAuto() {
        assertTrue(PrefixGuiSlots.isAuto(null));
        assertTrue(PrefixGuiSlots.isAuto(""));
        assertTrue(PrefixGuiSlots.isAuto("  "));
        assertFalse(PrefixGuiSlots.isAuto("auto"));
    }
}
