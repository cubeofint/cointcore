package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeDenyConfigTest {
    @Test
    void emptyDefaultDoesNotDeny() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson(
                """
                        {
                          "denyBlockIds": [],
                          "denyBlockTags": []
                        }
                        """
        );
        assertTrue(parsed.denyBlockIds().isEmpty());
        assertTrue(parsed.denyBlockTags().isEmpty());
        assertFalse(parsed.containsId("minecraft:furnace"));
    }

    @Test
    void denyBlockIdsAreCachedForLookup() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson(
                """
                        {
                          "denyBlockIds": ["justdirethings:clickert1", "sfm:manager", ""]
                        }
                        """
        );
        assertEquals(2, parsed.denyBlockIds().size());
        assertTrue(parsed.containsId("justdirethings:clickert1"));
        assertTrue(parsed.containsId("sfm:manager"));
        assertFalse(parsed.containsId("minecraft:furnace"));
    }

    @Test
    void denyBlockTagsStripHashPrefix() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson(
                """
                        {
                          "denyBlockTags": ["#cointcore:custom_no_surge", "minecraft:impermeable"]
                        }
                        """
        );
        assertEquals(List.of("cointcore:custom_no_surge", "minecraft:impermeable"), parsed.denyBlockTags());
    }

    @Test
    void leftoverKeysAreIgnored() {
        SoulSurgeDenyParse.Parsed parsed = assertDoesNotThrow(() -> SoulSurgeDenyParse.parseJson(
                """
                        {
                          "deprecated": true,
                          "unused": true,
                          "denyBlockIds": ["mekanism:enrichment_chamber"],
                          "denyBlockNamespaces": ["mekanism", "ae2"],
                          "denyBlockTags": ["#cointcore:custom_no_surge"]
                        }
                        """
        ));
        assertEquals(1, parsed.denyBlockIds().size());
        assertTrue(parsed.containsId("mekanism:enrichment_chamber"));
        assertEquals(1, parsed.denyBlockTags().size());
        assertEquals("cointcore:custom_no_surge", parsed.denyBlockTags().get(0));
    }

    @Test
    void emptyObjectIsTolerant() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson("{}");
        assertTrue(parsed.denyBlockIds().isEmpty());
        assertTrue(parsed.denyBlockTags().isEmpty());
        assertFalse(parsed.containsId("minecraft:furnace"));
    }
}
