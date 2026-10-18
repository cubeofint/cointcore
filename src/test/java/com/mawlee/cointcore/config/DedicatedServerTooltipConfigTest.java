package com.mawlee.cointcore.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DedicatedServerTooltipConfigTest {
    @Test
    void crashFixesDefaultEnabled() throws Exception {
        Path root = Path.of("src/main/java/com/mawlee/cointcore/config");
        String fs = Files.readString(root.resolve("FunctionalStorageArmorySearchConfig.java"));
        String relics = Files.readString(root.resolve("RelicsServerTooltipConfig.java"));
        String lag = Files.readString(root.resolve("LagFixesConfigs.java"));
        assertTrue(fs.contains("boolean enabled = true"));
        assertTrue(relics.contains("boolean enabled = true"));
        assertTrue(lag.contains("functionalstorage_armory_search"));
        assertTrue(lag.contains("relics_server_tooltip"));
    }
}
