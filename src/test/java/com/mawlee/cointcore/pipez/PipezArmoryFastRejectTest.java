package com.mawlee.cointcore.pipez;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipezArmoryFastRejectTest {
    @BeforeEach
    void reset() {
        PipezArmoryFastReject.resetSkippedInserts();
    }

    @Test
    void skipsUncertifiedNonEmptyStackIntoArmoryWhenEnabled() {
        assertTrue(PipezArmoryFastReject.shouldSkipInsert(true, true, false, false));
    }

    @Test
    void doesNotSkipCertifiedOrEmptyOrDisabled() {
        assertFalse(PipezArmoryFastReject.shouldSkipInsert(false, true, false, false));
        assertFalse(PipezArmoryFastReject.shouldSkipInsert(true, false, false, false));
        assertFalse(PipezArmoryFastReject.shouldSkipInsert(true, true, true, false));
        assertFalse(PipezArmoryFastReject.shouldSkipInsert(true, true, false, true));
    }

    @Test
    void matchesArmoryHandlerByClassNameIncludingSubclass() {
        assertTrue(PipezArmoryFastReject.typeIsArmoryCabinet(NamedArmoryHandler.class));
        assertTrue(PipezArmoryFastReject.isArmoryCabinet(new NamedArmoryHandler()));
        assertFalse(PipezArmoryFastReject.isArmoryCabinet(new Object()));
        assertFalse(PipezArmoryFastReject.isArmoryCabinet(null));
        assertFalse(PipezArmoryFastReject.typeIsArmoryCabinet(Object.class));
    }

    @Test
    void skippedCounterAndStatusLine() {
        assertEquals(0L, PipezArmoryFastReject.skippedInserts());
        PipezArmoryFastReject.recordSkippedInsert();
        PipezArmoryFastReject.recordSkippedInsert();
        assertEquals(2L, PipezArmoryFastReject.skippedInserts());
        assertEquals(
                "Pipez armory fast-reject: enabled=true skipped=2",
                PipezArmoryFastReject.statusLine(true)
        );
        assertEquals(
                "Pipez armory fast-reject: enabled=false skipped=2",
                PipezArmoryFastReject.statusLine(false)
        );
    }

    /**
     * Same binary name as Functional Storage 1.5.8 so {@link Class#getName()} matches production.
     */
    public static final class NamedArmoryHandler extends com.buuz135.functionalstorage.inventory.ArmoryCabinetInventoryHandler {
    }
}
