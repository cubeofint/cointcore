package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModularBeesRsConfigTest {
    @Test void oldSettingsUseBatchedDefaultsAndReloadCanDisableTransfersOrTuneTheInterval() {
        var gson = new Gson();
        try {
            ModularBeesRsConfig.applySection(gson.fromJson("{}", ModularBeesRsConfig.Data.class));
            assertTrue(ModularBeesRsConfig.isEnabled());
            assertEquals(100, ModularBeesRsConfig.getIntervalTicks());
            ModularBeesRsConfig.applySection(gson.fromJson("{\"enabled\":false,\"transfer_interval_ticks\":20}", ModularBeesRsConfig.Data.class));
            assertFalse(ModularBeesRsConfig.isEnabled());
            assertEquals(20, ModularBeesRsConfig.getIntervalTicks());
            ModularBeesRsConfig.applySection(gson.fromJson("{\"transfer_interval_ticks\":0}", ModularBeesRsConfig.Data.class));
            assertEquals(1, ModularBeesRsConfig.getIntervalTicks());
            ModularBeesRsConfig.applySection(gson.fromJson("{\"transfer_interval_ticks\":999999}", ModularBeesRsConfig.Data.class));
            assertEquals(1200, ModularBeesRsConfig.getIntervalTicks());
        } finally {
            ModularBeesRsConfig.applySection(ModularBeesRsConfig.defaultFileData());
        }
    }
}
