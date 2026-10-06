package com.mawlee.cointcore.watchdog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Deletes old files by max age and/or max total size (oldest first).
 * Pure filesystem logic — safe to unit-test without Minecraft.
 */
public final class RetentionPolicy {
    private RetentionPolicy() {
    }

    public record Result(int deletedFiles, long deletedBytes, long remainingBytes, int remainingFiles) {
    }

    public record FileEntry(Path path, Instant modified, long size) {
    }

    public static Result apply(
            Path directory,
            Instant now,
            Duration maxAge,
            long maxTotalBytes,
            Predicate<Path> fileFilter
    ) throws IOException {
        if (directory == null || !Files.isDirectory(directory)) {
            return new Result(0, 0L, 0L, 0);
        }
        if ((maxAge == null || maxAge.isNegative() || maxAge.isZero())
                && maxTotalBytes <= 0L) {
            return summarize(listFiles(directory, fileFilter));
        }

        List<FileEntry> files = listFiles(directory, fileFilter);
        files.sort(Comparator.comparing(FileEntry::modified).thenComparing(entry -> entry.path().toString()));

        long deletedBytes = 0L;
        int deletedFiles = 0;
        List<FileEntry> kept = new ArrayList<>(files.size());

        Instant cutoff = maxAge != null && !maxAge.isZero() && !maxAge.isNegative()
                ? now.minus(maxAge)
                : null;

        for (FileEntry entry : files) {
            boolean tooOld = cutoff != null && entry.modified().isBefore(cutoff);
            if (tooOld) {
                if (deleteQuietly(entry.path())) {
                    deletedFiles++;
                    deletedBytes += entry.size();
                }
            } else {
                kept.add(entry);
            }
        }

        if (maxTotalBytes > 0L) {
            long total = 0L;
            for (FileEntry entry : kept) {
                total += entry.size();
            }
            int index = 0;
            while (total > maxTotalBytes && index < kept.size()) {
                FileEntry entry = kept.get(index);
                if (deleteQuietly(entry.path())) {
                    deletedFiles++;
                    deletedBytes += entry.size();
                    total -= entry.size();
                }
                index++;
            }
            if (index > 0) {
                kept = kept.subList(index, kept.size());
            }
        }

        long remainingBytes = 0L;
        for (FileEntry entry : kept) {
            remainingBytes += entry.size();
        }
        return new Result(deletedFiles, deletedBytes, remainingBytes, kept.size());
    }

    public static List<FileEntry> listFiles(Path directory, Predicate<Path> fileFilter) throws IOException {
        List<FileEntry> files = new ArrayList<>();
        if (directory == null || !Files.isDirectory(directory)) {
            return files;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> fileFilter == null || fileFilter.test(path))
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            files.add(new FileEntry(path, attrs.lastModifiedTime().toInstant(), attrs.size()));
                        } catch (IOException ignored) {
                            // skip unreadable entries
                        }
                    });
        }
        return files;
    }

    private static Result summarize(List<FileEntry> files) {
        long bytes = 0L;
        for (FileEntry entry : files) {
            bytes += entry.size();
        }
        return new Result(0, 0L, bytes, files.size());
    }

    private static boolean deleteQuietly(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException exception) {
            return false;
        }
    }
}
