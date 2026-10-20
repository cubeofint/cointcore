package com.mawlee.cointcore.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiniMessageLegacyTest {
    @Test
    void hexColourAndResetFromLuckPermsPrefix() {
        assertEquals("§x§0§0§a§a§0§0[Модератор]§r ", MiniMessageLegacy.toLegacy("<#00AA00>[Модератор]<reset> "));
    }

    @Test
    void namedColoursDecorationsAndClosingTags() {
        assertEquals("§f§lName§r", MiniMessageLegacy.toLegacy("<white><bold>Name</bold>"));
        assertEquals("§x§f§f§0§0§0§0A", MiniMessageLegacy.toLegacy("<color:#ff0000>A"));
        assertEquals("§x§1§1§2§2§3§3G", MiniMessageLegacy.toLegacy("<gradient:#112233:#445566>G"));
    }

    @Test
    void legacyAndUnknownTagsPassThrough() {
        assertEquals("&a[A]", MiniMessageLegacy.toLegacy("&a[A]"));
        assertEquals("<hover:show_text:x>A", MiniMessageLegacy.toLegacy("<hover:show_text:x>A"));
        assertEquals("<#12>", MiniMessageLegacy.toLegacy("<#12>"));
        assertEquals("", MiniMessageLegacy.toLegacy(null));
    }
}
