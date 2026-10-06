package com.mawlee.cointcore.invsee;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Write-to-temp then replace, keeping {@code <name>_old} as a backup of the previous file.
 */
public final class AtomicFileWriter {
    private AtomicFileWriter() {
    }

    public static void write(Path target, byte[] data) throws IOException {
        Path directory = target.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }

        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Path backup = backupPath(target);
        Files.write(temp, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try {
            if (Files.exists(target)) {
                move(target, backup);
            }
            move(temp, target);
        } catch (IOException exception) {
            Files.deleteIfExists(temp);
            throw exception;
        }
    }

    public static Path backupPath(Path target) {
        String fileName = target.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            return target.resolveSibling(fileName.substring(0, dot) + fileName.substring(dot) + "_old");
        }
        return target.resolveSibling(fileName + "_old");
    }

    private static void move(Path source, Path dest) throws IOException {
        try {
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
