package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CurrencyMovementConfigTest {
    @Test
    void defaultsKeepSiteDeliveryOff() {
        CurrencyMovementConfig.Settings settings = CurrencyMovementConfig.Settings.from(CurrencyMovementConfig.FileData.defaults());
        assertFalse(settings.enabled());
        assertFalse(settings.siteQueueEnabled());
        assertFalse(settings.siteMovementsEnabled());
        assertEquals(100, settings.siteMovementsBatch());
        assertEquals(5, settings.siteQueuePollSeconds());
    }

    @Test
    void clampsBatchSize() {
        CurrencyMovementConfig.FileData data = CurrencyMovementConfig.FileData.defaults();
        data.siteMovementsEnabled = true;
        data.siteMovementsBatch = 0;
        assertEquals(100, CurrencyMovementConfig.Settings.from(data).siteMovementsBatch());
        data.siteMovementsBatch = 999;
        assertEquals(200, CurrencyMovementConfig.Settings.from(data).siteMovementsBatch());
    }
}
