package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeNestedKindTest {
    @Test
    void backpackWinsOverContainer() {
        assertEquals(
                InvSeeNestedKind.BACKPACK,
                InvSeeNestedKind.classify(true, true, true, true)
        );
    }

    @Test
    void bundleAndShulkerClassified() {
        assertEquals(InvSeeNestedKind.BUNDLE, InvSeeNestedKind.classify(false, true, false, false));
        assertEquals(InvSeeNestedKind.CONTAINER, InvSeeNestedKind.classify(false, false, true, false));
        assertEquals(InvSeeNestedKind.CONTAINER, InvSeeNestedKind.classify(false, false, false, true));
        assertEquals(InvSeeNestedKind.NONE, InvSeeNestedKind.classify(false, false, false, false));
    }

    @Test
    void noneDoesNotOpenMenu() {
        assertFalse(InvSeeNestedKind.NONE.opensMenu());
        assertTrue(InvSeeNestedKind.CONTAINER.opensMenu());
        assertTrue(InvSeeNestedKind.BUNDLE.opensMenu());
        assertTrue(InvSeeNestedKind.BACKPACK.opensMenu());
    }
}
