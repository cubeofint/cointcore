package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegratedTransferPerfConfigTest {
    @Test void oldConfigsEnableTheFastPathAndExplicitFalseIsPreserved() {
        try {
            var gson = new Gson();
            IntegratedTransferPerfConfig.applySection(gson.fromJson("{}", IntegratedTransferPerfConfig.Data.class));
            assertTrue(IntegratedTransferPerfConfig.isEmptySourceFastPath());
            IntegratedTransferPerfConfig.applySection(gson.fromJson("{\"empty_source_fast_path\":false}", IntegratedTransferPerfConfig.Data.class));
            assertFalse(IntegratedTransferPerfConfig.isEmptySourceFastPath());
            var root = JsonParser.parseString("{\"integrated_tunnels\":{\"empty_source_fast_path\":false},\"ae2\":{\"custom\":7}}").getAsJsonObject();
            assertFalse(ConfigMergeSupport.addMissingSectionValues(root, "integrated_tunnels", IntegratedTransferPerfConfig.defaultFileData()));
            assertFalse(root.getAsJsonObject("integrated_tunnels").get("empty_source_fast_path").getAsBoolean());
            assertEquals(7, root.getAsJsonObject("ae2").get("custom").getAsInt());
        } finally {
            IntegratedTransferPerfConfig.applySection(IntegratedTransferPerfConfig.defaultFileData());
        }
    }
}
