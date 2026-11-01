package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MekanismPerfConfigTest {
    @Test void lookupCanBeDisabledAndOldConfigsReceiveTheEnabledDefault() {
        try {
            var gson = new Gson();
            MekanismPerfConfig.applySection(gson.fromJson("{}", MekanismPerfConfig.Data.class));
            assertTrue(MekanismPerfConfig.isDirectItemContainerLookup());
            MekanismPerfConfig.applySection(gson.fromJson("{\"direct_item_container_lookup\":false}", MekanismPerfConfig.Data.class));
            assertFalse(MekanismPerfConfig.isDirectItemContainerLookup());
        } finally {
            MekanismPerfConfig.applySection(MekanismPerfConfig.defaultFileData());
        }
    }

    @Test void addingDefaultsPreservesExistingOptionsAndOtherSections() {
        var root = JsonParser.parseString("{\"ae2\":{\"storage_cache_interval_ticks\":9},\"mekanism\":{\"custom\":17}}").getAsJsonObject();
        assertTrue(ConfigMergeSupport.addMissingSectionValues(root, "mekanism", MekanismPerfConfig.defaultFileData()));
        assertEquals(9, root.getAsJsonObject("ae2").get("storage_cache_interval_ticks").getAsInt());
        assertEquals(17, root.getAsJsonObject("mekanism").get("custom").getAsInt());
        var section = root.getAsJsonObject("mekanism"); section.addProperty("direct_item_container_lookup", false);
        assertFalse(ConfigMergeSupport.addMissingSectionValues(root, "mekanism", MekanismPerfConfig.defaultFileData()));
        assertFalse(section.get("direct_item_container_lookup").getAsBoolean());
    }
}
