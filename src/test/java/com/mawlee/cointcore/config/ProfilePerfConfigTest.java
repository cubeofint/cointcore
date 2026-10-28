package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfilePerfConfigTest {
    @Test void newOptionsAreWrittenWithoutOverwritingExistingValuesOrNestedMaps() {
        var root = JsonParser.parseString("{\"ae2\":{\"storage_cache_interval_ticks\":9,\"custom\":{\"keep\":false}}}").getAsJsonObject();
        assertTrue(ConfigMergeSupport.addMissingSectionValues(root, "ae2", Ae2PerfConfig.defaultFileData()));
        var ae2 = root.getAsJsonObject("ae2");
        assertEquals(20, ae2.get("automatic_crafting_retry_ticks").getAsInt());
        assertEquals(9, ae2.get("storage_cache_interval_ticks").getAsInt());
        assertFalse(ae2.getAsJsonObject("custom").get("keep").getAsBoolean());
        ae2.addProperty("automatic_crafting_retry_ticks", 1);
        assertFalse(ConfigMergeSupport.addMissingSectionValues(root, "ae2", Ae2PerfConfig.defaultFileData()));
        assertEquals(1, ae2.get("automatic_crafting_retry_ticks").getAsInt());
    }

    @Test void oldConfigsGetDefaultsAndBothOptimizationsCanBeDisabledSeparately() {
        var gson = new Gson();
        try {
            Ae2PerfConfig.applySection(gson.fromJson("{}", Ae2PerfConfig.Data.class));
            assertEquals(20, Ae2PerfConfig.getAutomaticCraftingRetryTicks());
            Ae2PerfConfig.applySection(gson.fromJson("{\"automatic_crafting_retry_ticks\":0}", Ae2PerfConfig.Data.class));
            assertEquals(1, Ae2PerfConfig.getAutomaticCraftingRetryTicks());
            LogisticsNetworksBudgetConfig.applySection(gson.fromJson("{\"enabled\":false}", LogisticsNetworksBudgetConfig.Data.class));
            assertTrue(LogisticsNetworksBudgetConfig.isOptimizeSignals());
            LogisticsNetworksBudgetConfig.applySection(gson.fromJson("{\"optimize_signals\":false}", LogisticsNetworksBudgetConfig.Data.class));
            assertFalse(LogisticsNetworksBudgetConfig.isOptimizeSignals());
        } finally {
            Ae2PerfConfig.applySection(Ae2PerfConfig.defaultFileData());
            LogisticsNetworksBudgetConfig.applySection(LogisticsNetworksBudgetConfig.defaultFileData());
        }
    }
}
