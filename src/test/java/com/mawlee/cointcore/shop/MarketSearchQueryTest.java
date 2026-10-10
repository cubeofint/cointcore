package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSearchQueryTest {
    private static final MarketSearchTarget SWORD = MarketSearchTarget.of(
            "Diamond Sword\nDiamond Sword",
            "When in main hand:\nAttack Damage\nSharpness V",
            "minecraft",
            "Minecraft",
            List.of("minecraft:swords", "swords"),
            "white",
            "minecraft:diamond_sword"
    );
    private static final MarketSearchTarget WOOL = MarketSearchTarget.of(
            "Red Wool",
            "Wool",
            "minecraft",
            "Minecraft",
            List.of("minecraft:wool", "wool"),
            "red",
            "minecraft:red_wool"
    );
    private static final MarketSearchTarget AE = MarketSearchTarget.of(
            "ME Drive",
            "Stores 10 storage cells",
            "ae2",
            "Applied Energistics 2",
            List.of("ae2:drive", "drive"),
            "blue",
            "ae2:drive"
    );

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(MarketSearchQuery.matches("", SWORD));
        assertTrue(MarketSearchQuery.matches("   ", WOOL));
        assertTrue(MarketSearchQuery.matches(null, AE));
    }

    @Test
    void plainWordMatchesNameOrTooltip() {
        assertTrue(MarketSearchQuery.matches("diamond", SWORD));
        assertTrue(MarketSearchQuery.matches("sharpness", SWORD));
        assertFalse(MarketSearchQuery.matches("diamond", WOOL));
        assertFalse(MarketSearchQuery.matches("pickaxe", SWORD));
    }

    @Test
    void atFiltersModNameOrId() {
        assertTrue(MarketSearchQuery.matches("@minecraft", SWORD));
        assertTrue(MarketSearchQuery.matches("@appliedenergistics", AE));
        assertTrue(MarketSearchQuery.matches("@ae2", AE));
        assertFalse(MarketSearchQuery.matches("@ae2", SWORD));
    }

    @Test
    void hashMatchesTooltipOnly() {
        assertTrue(MarketSearchQuery.matches("#sharpness", SWORD));
        assertFalse(MarketSearchQuery.matches("#sharpness", WOOL));
        assertFalse(MarketSearchQuery.matches("#diamond", SWORD));
    }

    @Test
    void dollarMatchesTags() {
        assertTrue(MarketSearchQuery.matches("$swords", SWORD));
        assertTrue(MarketSearchQuery.matches("$minecraft:wool", WOOL));
        assertFalse(MarketSearchQuery.matches("$logs", SWORD));
    }

    @Test
    void caretMatchesColor() {
        assertTrue(MarketSearchQuery.matches("^red", WOOL));
        assertFalse(MarketSearchQuery.matches("^red", SWORD));
    }

    @Test
    void leadingMinusNegates() {
        assertFalse(MarketSearchQuery.matches("-diamond", SWORD));
        assertTrue(MarketSearchQuery.matches("-diamond", WOOL));
        assertFalse(MarketSearchQuery.matches("-@minecraft", SWORD));
        assertTrue(MarketSearchQuery.matches("-@ae2", SWORD));
        assertTrue(MarketSearchQuery.hasNegation("-sword"));
        assertFalse(MarketSearchQuery.hasNegation("sword|axe"));
    }

    @Test
    void pipeIsOr() {
        assertTrue(MarketSearchQuery.matches("iron|diamond", SWORD));
        assertTrue(MarketSearchQuery.matches("iron|red", WOOL));
        assertFalse(MarketSearchQuery.matches("iron|gold", SWORD));
        assertTrue(MarketSearchQuery.matches("@ae|#stores", AE));
    }

    @Test
    void quotedPhraseStaysTogether() {
        MarketSearchTarget oak = MarketSearchTarget.of(
                "Dark Oak Planks",
                "Planks",
                "minecraft",
                "Minecraft",
                List.of("minecraft:planks", "planks"),
                "brown",
                "minecraft:dark_oak_planks"
        );
        assertTrue(MarketSearchQuery.matches("\"dark oak\"", oak));
        assertFalse(MarketSearchQuery.matches("\"dark oak\"", SWORD));
        assertTrue(MarketSearchQuery.matches("\"dark oak\" plank", oak));
    }

    @Test
    void wordsAreAndWithinAGroup() {
        assertTrue(MarketSearchQuery.matches("@minecraft sword", SWORD));
        assertFalse(MarketSearchQuery.matches("@minecraft wool", SWORD));
        assertTrue(MarketSearchQuery.matches("diamond -pick", SWORD));
        assertFalse(MarketSearchQuery.matches("diamond -sword", SWORD));
    }

    @Test
    void resourceIdPrefix() {
        assertTrue(MarketSearchQuery.matches("&diamond_sword", SWORD));
        assertFalse(MarketSearchQuery.matches("&red_wool", SWORD));
    }

    @Test
    void parseSplitsOrAndPrefixes() {
        List<List<MarketSearchQuery.Term>> groups = MarketSearchQuery.parse("sword| -@ae2 #tip");
        assertEquals(2, groups.size());
        assertEquals(MarketSearchQuery.Kind.NAME, groups.getFirst().getFirst().kind());
        assertEquals("sword", groups.getFirst().getFirst().text());
        assertEquals(2, groups.get(1).size());
        assertTrue(groups.get(1).getFirst().negated());
        assertEquals(MarketSearchQuery.Kind.MOD, groups.get(1).getFirst().kind());
        assertEquals(MarketSearchQuery.Kind.TOOLTIP, groups.get(1).get(1).kind());
    }
}
