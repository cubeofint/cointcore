package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankTableTest {
    @Test
    void defaultsCoverAllNineAtmRanks() {
        QuestRankTable table = QuestRankTable.defaults();
        assertEquals(9, table.size());
        assertEquals("darwinawards", table.groupFor(QuestRankIds.parseHex("539D2768DC388BB2").orElseThrow()).orElseThrow());
        assertEquals("explorer", table.groupFor(QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow()).orElseThrow());
        assertEquals("engineer", table.groupFor(QuestRankIds.parseHex("03610B08578C5710").orElseThrow()).orElseThrow());
        assertEquals("atm", table.groupFor(QuestRankIds.parseHex("4E6C254EF6E26DB2").orElseThrow()).orElseThrow());
        assertEquals("arcanist", table.groupFor(QuestRankIds.parseHex("5F2E99EA50A3E1CA").orElseThrow()).orElseThrow());
        assertEquals("bosskiller", table.groupFor(QuestRankIds.parseHex("668583DA736FE3EE").orElseThrow()).orElseThrow());
        assertEquals("titan", table.groupFor(QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow()).orElseThrow());
        assertEquals("jesus", table.groupFor(QuestRankIds.parseHex("22E4E7DE8AF39F1D").orElseThrow()).orElseThrow());
        assertEquals("godofatm", table.groupFor(QuestRankIds.parseHex("330D102D30E63E2C").orElseThrow()).orElseThrow());
    }

    @Test
    void parseNormalizesGroupsAndSkipsBadRows() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("539D2768DC388BB2", " DarwinAwards ");
        raw.put("not-a-quest", "explorer");
        raw.put("3FABD06F3F6D3BC3", "  ");
        raw.put(null, "atm");
        QuestRankTable table = QuestRankTable.parse(raw);
        assertEquals(1, table.size());
        assertEquals("darwinawards", table.groupFor(QuestRankIds.parseHex("539D2768DC388BB2").orElseThrow()).orElseThrow());
        assertTrue(table.groupFor(1L).isEmpty());
    }

    @Test
    void emptyAndNullMapsStayEmpty() {
        assertTrue(QuestRankTable.parse(null).isEmpty());
        assertTrue(QuestRankTable.parse(Map.of()).isEmpty());
    }
}
