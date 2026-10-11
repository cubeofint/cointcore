package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeDenyConfigTest {
    @Test
    void deprecatedStubParses() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson(
                """
                        {
                          "deprecated": true,
                          "unused": true,
                          "denyBlockIds": [],
                          "denyBlockNamespaces": [],
                          "denyBlockTags": []
                        }
                        """
        );
        assertTrue(parsed.deprecated());
        assertTrue(parsed.denyBlockIds().isEmpty());
        assertTrue(parsed.denyBlockTags().isEmpty());
    }

    @Test
    void leftoverDenyListsDoNotCrash() {
        SoulSurgeDenyParse.Parsed parsed = assertDoesNotThrow(() -> SoulSurgeDenyParse.parseJson(
                """
                        {
                          "denyBlockIds": ["justdirethings:clickert1", "sfm:manager"],
                          "denyBlockNamespaces": ["mekanism", "ae2"],
                          "denyBlockTags": ["#cointcore:soul_surge_no_accelerate"]
                        }
                        """
        ));
        assertFalse(parsed.deprecated());
        assertEquals(2, parsed.denyBlockIds().size());
        assertEquals(2, parsed.denyBlockNamespaces().size());
        assertEquals(1, parsed.denyBlockTags().size());
    }

    @Test
    void emptyObjectIsTolerant() {
        SoulSurgeDenyParse.Parsed parsed = SoulSurgeDenyParse.parseJson("{}");
        assertFalse(parsed.deprecated());
        assertTrue(parsed.denyBlockIds().isEmpty());
    }
}
