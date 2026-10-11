package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SoulSurgeDebugTest {
    @Test
    void surgeLineShowsFacingTargetAndSouls() {
        assertEquals(
                "Soul Surge at 4 65 7 facing=up target=4 64 7 souls=yes",
                SoulSurgeDebug.surgeLine(4, 65, 7, "up", 4, 64, 7, true)
        );
        assertEquals(
                "Soul Surge at 4 65 7 facing=up target=4 64 7 souls=no",
                SoulSurgeDebug.surgeLine(4, 65, 7, "up", 4, 64, 7, false)
        );
    }

    @Test
    void ownerLineShowsClaimAndActive() {
        assertEquals(
                "Target 4 64 7 claimed by surge at 4 63 7 active=yes",
                SoulSurgeDebug.ownerLine(4, 64, 7, 4, 63, 7, true)
        );
        assertEquals(
                "Target 4 64 7 claimed by surge at 4 63 7 active=no",
                SoulSurgeDebug.ownerLine(4, 64, 7, 4, 63, 7, false)
        );
    }

    @Test
    void noOwnerLine() {
        assertEquals("No Soul Surge claim at 1 2 3", SoulSurgeDebug.noOwnerLine(1, 2, 3));
    }

    @Test
    void missingSurgeHasNoSouls() {
        assertFalse(SoulSurgeSouls.hasSouls(null));
        assertEquals(0, SoulSurgeSouls.tickingTime(null));
        assertEquals(0, SoulSurgeSouls.networkSoulAmount(null));
        assertFalse(SoulSurgeSouls.hasSouls(new Object()));
    }
}
