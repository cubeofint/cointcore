package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulSurgeDebugTest {
    @Test
    void surgeLineShowsFacingAndTarget() {
        assertEquals(
                "Soul Surge at 4 65 7 facing=up target=4 64 7",
                SoulSurgeDebug.surgeLine(4, 65, 7, "up", 4, 64, 7)
        );
    }

    @Test
    void ownerLineShowsClaim() {
        assertEquals(
                "Target 4 64 7 claimed by surge at 4 63 7",
                SoulSurgeDebug.ownerLine(4, 64, 7, 4, 63, 7)
        );
    }

    @Test
    void noOwnerLine() {
        assertEquals("No Soul Surge claim at 1 2 3", SoulSurgeDebug.noOwnerLine(1, 2, 3));
    }
}
