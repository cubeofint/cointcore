package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnerLootConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void defaultsMatchRequirements() {
        SpawnerLootConfig.Settings settings = SpawnerLootConfig.Settings.from(new SpawnerLootConfig.FileData());
        assertTrue(settings.enabled);
        assertTrue(settings.skipFinalizeMobSpawn);
        assertTrue(settings.affixDropsEnabled);
        assertEquals(1.0F, settings.affixChanceMultiplier);
        assertEquals(8, settings.affixMaxPerMinute);
        assertFalse(settings.eliteDrops);
    }

    @Test
    void nullFileUsesDefaults() {
        SpawnerLootConfig.Settings settings = SpawnerLootConfig.Settings.from(null);
        assertTrue(settings.enabled);
        assertTrue(settings.affixDropsEnabled);
        assertFalse(settings.eliteDrops);
    }

    @Test
    void disabledMasterSwitchIsHonored() {
        SpawnerLootConfig.FileData data = GSON.fromJson("{\"enabled\":false}", SpawnerLootConfig.FileData.class);
        SpawnerLootConfig.Settings settings = SpawnerLootConfig.Settings.from(data);
        assertFalse(settings.enabled);
        assertTrue(settings.skipFinalizeMobSpawn);
        assertTrue(settings.affixDropsEnabled);
    }

    @Test
    void affixSubsectionParses() {
        SpawnerLootConfig.FileData data = GSON.fromJson(
                """
                        {
                          "enabled": true,
                          "skip_finalize_mob_spawn": false,
                          "apotheosis_affix_drops": {
                            "enabled": false,
                            "chance_multiplier": 2.5,
                            "max_per_minute": 3,
                            "elite_drops": true
                          }
                        }
                        """,
                SpawnerLootConfig.FileData.class
        );
        SpawnerLootConfig.Settings settings = SpawnerLootConfig.Settings.from(data);
        assertTrue(settings.enabled);
        assertFalse(settings.skipFinalizeMobSpawn);
        assertFalse(settings.affixDropsEnabled);
        assertEquals(2.5F, settings.affixChanceMultiplier);
        assertEquals(3, settings.affixMaxPerMinute);
        assertTrue(settings.eliteDrops);
    }
}
