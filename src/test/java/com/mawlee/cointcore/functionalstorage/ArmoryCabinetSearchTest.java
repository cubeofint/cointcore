package com.mawlee.cointcore.functionalstorage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmoryCabinetSearchTest {
    @Test
    void matchesRegistryPathAndId() {
        assertTrue(ArmoryCabinetSearch.matchesQuery(
                "",
                "minecraft:diamond_sword",
                "diamond_sword",
                "item.minecraft.diamond_sword",
                "sword"
        ));
        assertTrue(ArmoryCabinetSearch.matchesQuery(
                "",
                "minecraft:diamond_sword",
                "diamond_sword",
                "item.minecraft.diamond_sword",
                "DIAMOND"
        ));
        assertTrue(ArmoryCabinetSearch.matchesQuery(
                "",
                "minecraft:diamond_sword",
                "diamond_sword",
                "item.minecraft.diamond_sword",
                "minecraft:diamond"
        ));
    }

    @Test
    void localizedHoverIsNotRequired() {
        assertFalse(ArmoryCabinetSearch.matchesQuery(
                "",
                "minecraft:diamond_sword",
                "diamond_sword",
                "item.minecraft.diamond_sword",
                "алмаз"
        ));
        assertTrue(ArmoryCabinetSearch.matchesQuery(
                "Алмазный меч",
                "minecraft:diamond_sword",
                "diamond_sword",
                "item.minecraft.diamond_sword",
                "алмаз"
        ));
    }

    @Test
    void emptyQueryMatches() {
        assertTrue(ArmoryCabinetSearch.matchesQuery("Stick", "minecraft:stick", "stick", "item.minecraft.stick", ""));
        assertTrue(ArmoryCabinetSearch.matchesQuery("Stick", "minecraft:stick", "stick", "item.minecraft.stick", null));
    }
}
