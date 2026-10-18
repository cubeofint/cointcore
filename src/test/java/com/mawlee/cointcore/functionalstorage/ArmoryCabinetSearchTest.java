package com.mawlee.cointcore.functionalstorage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmoryCabinetSearchTest {
    @Test
    void matchesHoverName() {
        assertTrue(ArmoryCabinetSearch.matchesQuery("Diamond Sword", "minecraft:diamond_sword", "sword"));
        assertTrue(ArmoryCabinetSearch.matchesQuery("Diamond Sword", "minecraft:diamond_sword", "DIAMOND"));
    }

    @Test
    void matchesRegistryId() {
        assertTrue(ArmoryCabinetSearch.matchesQuery("Алмазный меч", "minecraft:diamond_sword", "diamond_sword"));
        assertTrue(ArmoryCabinetSearch.matchesQuery("Алмазный меч", "minecraft:diamond_sword", "minecraft:diamond"));
        assertFalse(ArmoryCabinetSearch.matchesQuery("Алмазный меч", "minecraft:diamond_sword", "netherite"));
    }

    @Test
    void emptyQueryMatches() {
        assertTrue(ArmoryCabinetSearch.matchesQuery("Stick", "minecraft:stick", ""));
        assertTrue(ArmoryCabinetSearch.matchesQuery("Stick", "minecraft:stick", null));
    }
}
