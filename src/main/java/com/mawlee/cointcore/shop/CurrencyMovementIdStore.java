package com.mawlee.cointcore.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Persists {@code next_id} / {@code accepted_up_to} outside wipe-able world data
 * ({@code config/cointcore/currency-movement-sequence.json}).
 */
public final class CurrencyMovementIdStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String FILE_NAME = "currency-movement-sequence.json";

    private static final Object LOCK = new Object();
    private static CurrencyMovementSequence.Snapshot snapshot = new CurrencyMovementSequence.Snapshot(1L, 0L);
    private static boolean loaded;
    private static Path file;

    private CurrencyMovementIdStore() {
    }

    public static void bind(Path path) {
        synchronized (LOCK) {
            if (path != null && path.equals(file) && loaded) {
                return;
            }
            file = path;
            loaded = false;
            snapshot = new CurrencyMovementSequence.Snapshot(1L, 0L);
            ensureLoaded();
        }
    }

    static void resetForTests() {
        synchronized (LOCK) {
            file = null;
            loaded = false;
            snapshot = new CurrencyMovementSequence.Snapshot(1L, 0L);
        }
    }

    public static CurrencyMovementSequence.Snapshot snapshot() {
        synchronized (LOCK) {
            ensureLoaded();
            return snapshot;
        }
    }

    /**
     * Combines world journal counters with the durable file and site {@code accepted_up_to}.
     */
    public static CurrencyMovementSequence.Snapshot reconcileWithWorld(long worldNextId, long worldSiteSentUpTo) {
        synchronized (LOCK) {
            ensureLoaded();
            CurrencyMovementSequence.Snapshot merged = CurrencyMovementSequence.merge(
                    worldNextId,
                    snapshot.nextId(),
                    worldSiteSentUpTo,
                    snapshot.acceptedUpTo()
            );
            persist(merged);
            return snapshot;
        }
    }

    public static void recordNextId(long nextId) {
        synchronized (LOCK) {
            ensureLoaded();
            persist(CurrencyMovementSequence.applyNextId(snapshot, nextId));
        }
    }

    public static void recordAcceptedUpTo(long acceptedUpTo) {
        synchronized (LOCK) {
            ensureLoaded();
            persist(CurrencyMovementSequence.applyAccepted(snapshot, acceptedUpTo));
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        snapshot = read(file);
        loaded = true;
    }

    private static void persist(CurrencyMovementSequence.Snapshot next) {
        if (next.nextId() == snapshot.nextId() && next.acceptedUpTo() == snapshot.acceptedUpTo() && loaded) {
            snapshot = next;
            return;
        }
        snapshot = next;
        loaded = true;
        write(file, snapshot);
    }

    static CurrencyMovementSequence.Snapshot read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return new CurrencyMovementSequence.Snapshot(1L, 0L);
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FileData data = GSON.fromJson(JsonParser.parseReader(reader), FileData.class);
            if (data == null) {
                return new CurrencyMovementSequence.Snapshot(1L, 0L);
            }
            return CurrencyMovementSequence.reconcile(data.nextId, data.acceptedUpTo);
        } catch (IOException | RuntimeException exception) {
            return new CurrencyMovementSequence.Snapshot(1L, 0L);
        }
    }

    static void write(Path path, CurrencyMovementSequence.Snapshot state) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            JsonObject root = new JsonObject();
            root.addProperty("next_id", state.nextId());
            root.addProperty("accepted_up_to", state.acceptedUpTo());
            try (Writer writer = Files.newBufferedWriter(
                    tmp,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            // Sequence file is best-effort; next start reconciles from world + last successful write.
        }
    }

    static final class FileData {
        @SerializedName("next_id")
        long nextId = 1L;
        @SerializedName("accepted_up_to")
        long acceptedUpTo;
    }
}
