package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowdedMobsConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void lagFixesDefaultsIncludeDisabledCrowdedMobsSection() throws Exception {
        Path lag = Path.of("src/main/java/com/mawlee/cointcore/config/LagFixesConfigs.java");
        String source = Files.readString(lag);
        assertTrue(source.contains("crowded_mobs"));
        assertTrue(source.contains("CrowdedMobsConfig"));
        CrowdedMobsConfig.Data data = new CrowdedMobsConfig.Data();
        assertFalse(data.enabled);
        assertEquals(8, data.sameTypeThreshold);
        assertEquals(1.0D, data.radiusBlocks);
        assertEquals(30, data.recheckIntervalTicks);
        assertEquals(15, data.aiIntervalTicks);
        assertTrue(data.disableEntityPush);
        assertTrue(data.throttleAi);
        assertFalse(data.includeVillagers);
        assertFalse(data.includeNamed);
        assertFalse(data.includeLeashed);
        assertFalse(data.includeBosses);
    }

    @Test
    void jsonMapsSnakeCaseFields() {
        CrowdedMobsConfig.Data data = GSON.fromJson(
                """
                        {
                          "enabled": true,
                          "same_type_threshold": 12,
                          "radius_blocks": 1.5,
                          "recheck_interval_ticks": 40,
                          "ai_interval_ticks": 10,
                          "include_named": true,
                          "allow_entity_types": ["minecraft:chicken"]
                        }
                        """,
                CrowdedMobsConfig.Data.class
        );
        assertTrue(data.enabled);
        assertEquals(12, data.sameTypeThreshold);
        assertEquals(1.5D, data.radiusBlocks);
        assertEquals(40, data.recheckIntervalTicks);
        assertEquals(10, data.aiIntervalTicks);
        assertTrue(data.includeNamed);
        assertEquals("minecraft:chicken", data.allowEntityTypes.getFirst());
    }
}
