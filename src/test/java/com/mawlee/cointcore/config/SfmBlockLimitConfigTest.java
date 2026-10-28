package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SfmBlockLimitConfigTest {
    @Test void missingDefaultsAreAddedWithoutOverwritingExistingSfmSettings() {
        var root = JsonParser.parseString("{\"sfm\":{\"minTimerIntervalTicks\":17},\"other\":{\"keep\":true}}")
                .getAsJsonObject();
        assertTrue(TickThrottleConfigs.addSfmBlockLimitDefault(root));
        assertEquals(0, root.getAsJsonObject("sfm").get("maxBoundBlocksPerManager").getAsInt());
        assertEquals(17, root.getAsJsonObject("sfm").get("minTimerIntervalTicks").getAsInt());
        assertTrue(root.getAsJsonObject("other").get("keep").getAsBoolean());
        root.getAsJsonObject("sfm").addProperty("maxBoundBlocksPerManager", 77);
        assertFalse(TickThrottleConfigs.addSfmBlockLimitDefault(root));
        assertEquals(77, root.getAsJsonObject("sfm").get("maxBoundBlocksPerManager").getAsInt());
    }

    @Test void blockLimitIsIndependentOfPerformanceThrottlingAndDefaultsToUnlimited() {
        var gson = new Gson();
        try {
            SfmPerfConfig.applySection(gson.fromJson("{}", SfmPerfConfig.FileData.class));
            assertEquals(0, SfmPerfConfig.getMaxBoundBlocksPerManager());
            SfmPerfConfig.applySection(gson.fromJson("{\"enabled\":false,\"maxBoundBlocksPerManager\":64}", SfmPerfConfig.FileData.class));
            assertFalse(SfmPerfConfig.isEnabled());
            assertEquals(64, SfmPerfConfig.getMaxBoundBlocksPerManager());
            SfmPerfConfig.applySection(gson.fromJson("{\"maxBoundBlocksPerManager\":-12}", SfmPerfConfig.FileData.class));
            assertEquals(0, SfmPerfConfig.getMaxBoundBlocksPerManager());
        } finally {
            SfmPerfConfig.applySection(SfmPerfConfig.defaultFileData());
        }
    }
}
