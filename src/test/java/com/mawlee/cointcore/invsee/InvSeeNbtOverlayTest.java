package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeNbtOverlayTest {
    @Test
    void overlaysInventoryKeys() {
        assertTrue(InvSeeNbtOverlay.shouldOverlay("Inventory"));
        assertTrue(InvSeeNbtOverlay.shouldOverlay("EnderItems"));
        assertTrue(InvSeeNbtOverlay.shouldOverlay("SelectedItemSlot"));
        assertTrue(InvSeeNbtOverlay.shouldOverlay("neoforge:attachments"));
        assertTrue(InvSeeNbtOverlay.shouldOverlay("curios:inventory"));
    }

    @Test
    void neverCopiesDimensionOrPosition() {
        assertFalse(InvSeeNbtOverlay.shouldOverlay("Dimension"));
        assertFalse(InvSeeNbtOverlay.shouldOverlay("Pos"));
        assertFalse(InvSeeNbtOverlay.shouldOverlay("Motion"));
        assertFalse(InvSeeNbtOverlay.shouldOverlay("Rotation"));
        assertTrue(InvSeeNbtOverlay.mustNeverCopy("Dimension"));
        assertTrue(InvSeeNbtOverlay.mustNeverCopy("Pos"));
    }

    @Test
    void itemLabelFormatsStacks() {
        assertEquals("empty", InvSeeItemLabels.describe(null, 1));
        assertEquals("empty", InvSeeItemLabels.describe("minecraft:air", 64));
        assertEquals("empty", InvSeeItemLabels.describe("minecraft:diamond", 0));
        assertEquals("minecraft:diamond x16", InvSeeItemLabels.describe("minecraft:diamond", 16));
    }
}
