package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeTabPolicyTest {
    @Test
    void accessoriesHiddenWhenModAbsent() {
        assertFalse(InvSeeTabPolicy.showAccessories(false));
        assertTrue(InvSeeTabPolicy.showAccessories(true));
    }

    @Test
    void gravesHiddenUnlessKnownModPresent() {
        assertFalse(InvSeeTabPolicy.showGraves(Set.of("ftbessentials", "curios")));
        assertTrue(InvSeeTabPolicy.showGraves(Set.of("yigd")));
        assertTrue(InvSeeTabPolicy.showGraves(Set.of("tombstone")));
        assertTrue(InvSeeTabPolicy.showGraves(Set.of("gravestone")));
    }

    @Test
    void maskOmitsDeniedAndMissingMods() {
        int mask = InvSeeTabPolicy.mask(
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                true
        );
        List<InvSeeTab> tabs = InvSeeTabPolicy.visible(mask);
        assertEquals(List.of(InvSeeTab.INVENTORY, InvSeeTab.ENDER, InvSeeTab.STATE), tabs);
        assertFalse(InvSeeTabPolicy.contains(mask, InvSeeTab.ACCESSORIES));
        assertFalse(InvSeeTabPolicy.contains(mask, InvSeeTab.FTB));
        assertFalse(InvSeeTabPolicy.contains(mask, InvSeeTab.GRAVES));
    }

    @Test
    void maskIncludesOptionalTabsWhenLoadedAndAllowed() {
        int mask = InvSeeTabPolicy.mask(
                true, true, true, true, true, true, true, true, true
        );
        assertEquals(InvSeeTab.values().length, InvSeeTabPolicy.visible(mask).size());
    }

    @Test
    void viewDeniedDropsTabEvenIfModLoaded() {
        int mask = InvSeeTabPolicy.mask(
                true, false, true, false, true, false, true, false, false
        );
        assertEquals(List.of(InvSeeTab.INVENTORY), InvSeeTabPolicy.visible(mask));
    }
}
