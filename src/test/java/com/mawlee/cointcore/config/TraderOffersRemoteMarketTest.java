package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderOffersRemoteMarketTest {
    @Test
    void remoteMarketDefaultsOnWhenUnset() {
        TraderOffersConfig.FileData data = new TraderOffersConfig.FileData();
        TraderOffersConfig.Loaded loaded = TraderOffersConfig.parse(data);
        assertTrue(loaded.remoteMarketEnabled());
    }

    @Test
    void remoteMarketCanBeDisabled() {
        TraderOffersConfig.FileData data = new TraderOffersConfig.FileData();
        data.remoteMarketEnabled = false;
        TraderOffersConfig.Loaded loaded = TraderOffersConfig.parse(data);
        assertFalse(loaded.remoteMarketEnabled());
    }
}
