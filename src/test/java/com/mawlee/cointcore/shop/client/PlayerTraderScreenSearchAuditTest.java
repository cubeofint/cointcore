package com.mawlee.cointcore.shop.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTraderScreenSearchAuditTest {
    @Test
    void buyerScreenDoesNotPullJeiOrRebuildOnSearch() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/mawlee/cointcore/shop/client/PlayerTraderScreen.java"));
        assertTrue(source.contains("GuiEditBoxes.create"));
        assertTrue(source.contains("refreshOfferChrome"));
        assertTrue(source.contains("setCanLoseFocus(false)"));
        assertFalse(source.contains("applyExternalQuery"));
        assertFalse(source.contains("containerTick"));
    }

    @Test
    void manageScreenUsesSharedWriteUnfocused() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/mawlee/cointcore/shop/client/PlayerTraderManageScreen.java"));
        assertTrue(source.contains("GuiEditBoxes.create"));
        assertTrue(source.contains("writeUnfocused"));
    }
}
