package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TickWatchdogConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void missingKeysKeepNewDefaultsWithoutBreakingOldFiles() {
        TickWatchdogConfig.FileData missing = GSON.fromJson("{\"enabled\":true}", TickWatchdogConfig.FileData.class);
        assertEquals(50.0D, missing.slowTickThresholdMs);
        assertEquals(40.0D, missing.sustainedAverageMs);
        assertEquals(60, missing.sustainedAverageWindowSeconds);

        TickWatchdogConfig.FileData existing = GSON.fromJson(
                "{\"enabled\":true,\"slowTickThresholdMs\":100,\"sustainedSlowTicks\":3}",
                TickWatchdogConfig.FileData.class
        );
        assertEquals(100.0D, existing.slowTickThresholdMs);
        assertEquals(40.0D, existing.sustainedAverageMs);
    }
}
