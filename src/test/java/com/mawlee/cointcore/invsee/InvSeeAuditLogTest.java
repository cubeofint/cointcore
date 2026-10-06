package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeAuditLogTest {
    @TempDir
    Path tempDir;

    @Test
    void formatsReadableActionLines() {
        String opened = InvSeeAuditLines.opened("Kate_Mod", "Steve", InvSeeSection.INVENTORY, false, true);
        assertEquals("Kate_Mod opened Steve section=inventory status=offline mode=can-edit", opened);
        assertTrue(InvSeeAuditLines.editMode("Kate_Mod", "Steve", true).contains("edit-on"));
        assertEquals(
                "Kate_Mod moved Steve section=inventory slot=12 minecraft:diamond x16 -> empty",
                InvSeeAuditLines.slotChange(
                        "Kate_Mod",
                        "Steve",
                        InvSeeSection.INVENTORY,
                        12,
                        "minecraft:diamond x16",
                        "empty"
                )
        );
        assertTrue(InvSeeAuditLines.closed("Kate_Mod", "Steve", InvSeeSection.INVENTORY)
                .contains("closed Steve section=inventory"));
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000009");
        assertTrue(InvSeeAuditLines.targetOnline(id, "Steve").contains("joined"));
        assertFalse(opened.contains("page"));
    }

    @Test
    void writesDailyLogFile() throws Exception {
        InvSeeAuditSink.bind(tempDir, null);
        InvSeeAuditSink.write("Kate_Mod opened Steve section=inventory status=offline mode=can-edit");
        List<Path> files = Files.list(tempDir).toList();
        assertEquals(1, files.size());
        assertTrue(files.getFirst().getFileName().toString().endsWith(".log"));
        String text = Files.readString(files.getFirst());
        assertTrue(text.contains("Kate_Mod opened Steve section=inventory"));
        assertTrue(text.startsWith("["));
        assertTrue(text.contains("UTC"));
    }
}
