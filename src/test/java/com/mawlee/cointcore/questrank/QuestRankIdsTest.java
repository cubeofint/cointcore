package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankIdsTest {
    @Test
    void parsesUppercaseHexIncludingLeadingZero() {
        long engineer = QuestRankIds.parseHex("03610B08578C5710").orElseThrow();
        assertEquals("3610B08578C5710", Long.toUnsignedString(engineer, 16).toUpperCase());
        assertEquals("3610B08578C5710", QuestRankIds.toHex(engineer).replaceFirst("^0+", ""));
        assertEquals("03610B08578C5710", String.format("%016X", engineer));
    }

    @Test
    void parsesPrefixedAndLowercaseHex() {
        long id = QuestRankIds.parseHex("0x539d2768dc388bb2").orElseThrow();
        assertEquals("539D2768DC388BB2", QuestRankIds.toHex(id));
        assertEquals(id, QuestRankIds.parseHex("539D2768DC388BB2").orElseThrow());
    }

    @Test
    void rejectsBlankAndInvalid() {
        assertTrue(QuestRankIds.parseHex(null).isEmpty());
        assertTrue(QuestRankIds.parseHex("  ").isEmpty());
        assertTrue(QuestRankIds.parseHex("0x").isEmpty());
        assertTrue(QuestRankIds.parseHex("not-hex").isEmpty());
        assertTrue(QuestRankIds.parseHex("539D2768DC388BB2ZZ").isEmpty());
    }
}
