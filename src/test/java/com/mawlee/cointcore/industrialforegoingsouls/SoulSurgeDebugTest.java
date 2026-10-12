package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulSurgeDebugTest {
    @Test
    void surgeLineShowsFacingTargetAndExtraBudget() {
        assertEquals(
                "Soul Surge at 4 65 7 facing=up target=4 64 7 extra=2/3",
                SoulSurgeDebug.surgeLine(4, 65, 7, "up", 4, 64, 7, 2, 3)
        );
        assertEquals(
                "Soul Surge at 4 65 7 facing=up target=4 64 7 extra=0/3",
                SoulSurgeDebug.surgeLine(4, 65, 7, "up", 4, 64, 7, 0, SoulSurgeAcceleration.MAX_EXTRA_TICKS)
        );
    }

    @Test
    void targetLineShowsExtraBudget() {
        assertEquals(
                "Target 4 64 7 extra=3/3",
                SoulSurgeDebug.targetLine(4, 64, 7, 3, 3)
        );
        assertEquals(
                "Target 1 2 3 extra=0/3",
                SoulSurgeDebug.targetLine(1, 2, 3, 0, SoulSurgeAcceleration.MAX_EXTRA_TICKS)
        );
    }
}
