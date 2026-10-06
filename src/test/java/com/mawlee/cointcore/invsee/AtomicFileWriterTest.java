package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFileWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesNewFile() throws Exception {
        Path target = tempDir.resolve("player.dat");
        AtomicFileWriter.write(target, "one".getBytes(StandardCharsets.UTF_8));
        assertEquals("one", Files.readString(target));
        assertFalse(Files.exists(AtomicFileWriter.backupPath(target)));
    }

    @Test
    void replacesExistingFileAndKeepsBackup() throws Exception {
        Path target = tempDir.resolve("player.dat");
        Files.writeString(target, "old");
        AtomicFileWriter.write(target, "new".getBytes(StandardCharsets.UTF_8));
        assertEquals("new", Files.readString(target));
        Path backup = AtomicFileWriter.backupPath(target);
        assertTrue(Files.exists(backup));
        assertEquals("old", Files.readString(backup));
        assertEquals(tempDir.resolve("player.dat_old"), backup);
        assertFalse(Files.exists(target.resolveSibling("player.dat.tmp")));
    }

    @Test
    void backupNameForExtensionlessFile() {
        Path target = tempDir.resolve("playerdata");
        assertEquals(tempDir.resolve("playerdata_old"), AtomicFileWriter.backupPath(target));
    }

    @Test
    void secondReplaceMovesPreviousValueToBackup() throws Exception {
        Path target = tempDir.resolve("uuid.dat");
        AtomicFileWriter.write(target, new byte[] {1, 2, 3});
        AtomicFileWriter.write(target, new byte[] {4, 5, 6});
        assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(target));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(AtomicFileWriter.backupPath(target)));
    }
}
