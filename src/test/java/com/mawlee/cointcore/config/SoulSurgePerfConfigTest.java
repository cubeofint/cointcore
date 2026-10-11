package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgePerfConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void defaultsAreEnabledSingleTarget() {
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(
                SoulSurgePerfConfig.FileData.defaults()
        );
        assertTrue(settings.enabled());
        assertTrue(settings.singleTarget());
    }

    @Test
    void nullFileUsesDefaults() {
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(null);
        assertTrue(settings.enabled());
        assertTrue(settings.singleTarget());
    }

    @Test
    void leftoverDenyAndCapFieldsAreIgnored() {
        SoulSurgePerfConfig.FileData data = GSON.fromJson(
                """
                        {
                          "enabled": true,
                          "maxAccelerationTicks": 1,
                          "respectChunkLimits": true,
                          "allowEntityAcceleration": true,
                          "allowBlockRandomTickAcceleration": true,
                          "denyBlockIds": ["mekanism:enrichment_chamber"],
                          "denyBlockTags": ["#cointcore:soul_surge_no_accelerate"]
                        }
                        """,
                SoulSurgePerfConfig.FileData.class
        );
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(data);
        assertTrue(settings.enabled());
        assertTrue(settings.singleTarget());
    }

    @Test
    void explicitFlagsAreHonored() {
        SoulSurgePerfConfig.FileData data = GSON.fromJson(
                """
                        {"enabled": false, "singleTarget": false}
                        """,
                SoulSurgePerfConfig.FileData.class
        );
        SoulSurgePerfConfig.Settings settings = SoulSurgePerfConfig.Settings.from(data);
        assertFalse(settings.enabled());
        assertFalse(settings.singleTarget());
    }
}
