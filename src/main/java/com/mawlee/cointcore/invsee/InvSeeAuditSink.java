package com.mawlee.cointcore.invsee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

final class InvSeeAuditSink {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private static Path directory;
    private static BiConsumer<Path, Exception> errorHandler;

    private InvSeeAuditSink() {
    }

    static void bind(Path logDirectory, BiConsumer<Path, Exception> onError) {
        directory = logDirectory;
        errorHandler = onError;
    }

    static void write(String message) {
        Path dir = directory;
        if (dir == null || message == null || message.isBlank()) {
            return;
        }

        Instant now = Instant.now();
        String line = "[" + TIME.format(now) + "] " + message + System.lineSeparator();
        Path file = dir.resolve(FILE_DAY.format(now) + ".log");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            if (errorHandler != null) {
                errorHandler.accept(file, exception);
            }
        }
    }
}
