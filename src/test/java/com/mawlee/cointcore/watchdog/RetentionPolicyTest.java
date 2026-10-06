package com.mawlee.cointcore.watchdog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionPolicyTest {
    @TempDir
    Path dir;

    @Test
    void deletesFilesOlderThanMaxAge() throws Exception {
        Path oldFile = dir.resolve("old.log");
        Path freshFile = dir.resolve("fresh.log");
        Files.writeString(oldFile, "old");
        Files.writeString(freshFile, "fresh");
        Instant now = Instant.parse("2026-10-06T00:00:00Z");
        Files.setLastModifiedTime(oldFile, java.nio.file.attribute.FileTime.from(now.minus(Duration.ofDays(10))));
        Files.setLastModifiedTime(freshFile, java.nio.file.attribute.FileTime.from(now.minus(Duration.ofHours(1))));

        RetentionPolicy.Result result = RetentionPolicy.apply(dir, now, Duration.ofDays(7), 0L, null);

        assertEquals(1, result.deletedFiles());
        assertFalse(Files.exists(oldFile));
        assertTrue(Files.exists(freshFile));
    }

    @Test
    void deletesOldestUntilSizeCap() throws Exception {
        Path a = dir.resolve("a.log");
        Path b = dir.resolve("b.log");
        Path c = dir.resolve("c.log");
        Files.writeString(a, "aaaa");
        Files.writeString(b, "bbbb");
        Files.writeString(c, "cccc");
        Instant now = Instant.parse("2026-10-06T00:00:00Z");
        Files.setLastModifiedTime(a, java.nio.file.attribute.FileTime.from(now.minus(Duration.ofDays(3))));
        Files.setLastModifiedTime(b, java.nio.file.attribute.FileTime.from(now.minus(Duration.ofDays(2))));
        Files.setLastModifiedTime(c, java.nio.file.attribute.FileTime.from(now.minus(Duration.ofDays(1))));

        RetentionPolicy.Result result = RetentionPolicy.apply(dir, now, Duration.ZERO, 6L, null);

        assertTrue(result.deletedFiles() >= 1);
        assertTrue(result.remainingBytes() <= 6L);
        assertTrue(Files.exists(c));
    }

    @Test
    void noOpWhenDirectoryMissing() throws Exception {
        RetentionPolicy.Result result = RetentionPolicy.apply(
                dir.resolve("missing"),
                Instant.now(),
                Duration.ofDays(1),
                10L,
                null
        );
        assertEquals(0, result.deletedFiles());
    }
}
