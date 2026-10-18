package com.mawlee.cointcore.neovitae;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeovitaePaletteRewriteTest {
    @Test
    void skipsPalettesWithoutRemappedIds() {
        Map<String, String> remaps = NeovitaePaletteRewrite.remapTable(new String[][] {
                {"neovitae:item_routing_node", "neovitae:routing_conduit"}
        });
        assertFalse(NeovitaePaletteRewrite.sectionNeedsRewrite(
                List.of("minecraft:stone", "minecraft:air", "minecraft:chest"),
                remaps
        ));
    }

    @Test
    void rewritesOnlyMatchingPaletteNames() {
        Map<String, String> remaps = NeovitaePaletteRewrite.remapTable(new String[][] {
                {"neovitae:item_routing_node", "neovitae:routing_conduit"}
        });
        assertTrue(NeovitaePaletteRewrite.sectionNeedsRewrite(
                List.of("minecraft:stone", "neovitae:item_routing_node"),
                remaps
        ));
        assertEquals("neovitae:routing_conduit", remaps.get("neovitae:item_routing_node"));
    }

    @Test
    void emptyRemapTableNeverRewrites() {
        assertFalse(NeovitaePaletteRewrite.sectionNeedsRewrite(List.of("neovitae:item_routing_node"), Map.of()));
        assertTrue(NeovitaePaletteRewrite.remapTable(null).isEmpty());
    }
}
