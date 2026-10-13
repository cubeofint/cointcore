package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgePerfConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void defaultsAreEnabled() {
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(
                SoulSurgePerfConfig.FileData.defaults()
        );
        assertTrue(settings.enabled());
    }

    @Test
    void nullFileUsesDefaults() {
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(null);
        assertTrue(settings.enabled());
    }

    @Test
    void leftoverFieldsAreIgnored() {
        SoulSurgePerfConfig.FileData data = GSON.fromJson(
                """
                        {
                          "enabled": true,
                          "singleTarget": false,
                          "maxAccelerationTicks": 1,
                          "respectChunkLimits": true,
                          "allowEntityAcceleration": true,
                          "allowBlockRandomTickAcceleration": true,
                          "denyBlockIds": ["mekanism:enrichment_chamber"],
                          "denyBlockTags": ["#cointcore:custom_no_surge"]
                        }
                        """,
                SoulSurgePerfConfig.FileData.class
        );
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(data);
        assertTrue(settings.enabled());
    }

    @Test
    void explicitEnabledFalseIsHonored() {
        SoulSurgePerfConfig.FileData data = GSON.fromJson(
                """
                        {"enabled": false}
                        """,
                SoulSurgePerfConfig.FileData.class
        );
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(data);
        assertFalse(settings.enabled());
    }
}
