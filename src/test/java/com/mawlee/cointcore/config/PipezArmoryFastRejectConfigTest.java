package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipezArmoryFastRejectConfigTest {
    @Test
    void lagFixesDefaultsIncludeEnabledPipezArmorySection() throws Exception {
        String lag = Files.readString(Path.of("src/main/java/com/mawlee/cointcore/config/LagFixesConfigs.java"));
        assertTrue(lag.contains("pipez_armory_fast_reject"));
        assertTrue(lag.contains("PipezArmoryFastRejectConfig"));
        PipezArmoryFastRejectConfig.Data data = new PipezArmoryFastRejectConfig.Data();
        assertTrue(data.enabled);
    }

    @Test
    void jsonMapsEnabledFlag() {
        PipezArmoryFastRejectConfig.Data data = new Gson().fromJson(
                """
                        { "enabled": false }
                        """,
                PipezArmoryFastRejectConfig.Data.class
        );
        assertFalse(data.enabled);
    }
}
