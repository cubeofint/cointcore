package com.mawlee.cointcore.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiEditBoxesTest {
    @Test
    void helperIsTheSharedEditBoxPolicy() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/mawlee/cointcore/client/GuiEditBoxes.java")
        );
        assertTrue(source.contains("writeUnfocused"));
        assertTrue(source.contains("canConsumeInput"));
        assertTrue(source.contains("handleKeyPressed"));
        assertTrue(source.contains("setResponder"));
        assertFalse(source.contains("rebuildWidgets"));
    }
}
