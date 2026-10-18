package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyMovementIdStoreTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void reset() {
        CurrencyMovementIdStore.resetForTests();
    }

    @Test
    void persistsNextIdAndAcceptedUpToOutsideWorldData() throws Exception {
        Path file = tempDir.resolve("currency-movement-sequence.json");
        CurrencyMovementIdStore.bind(file);

        CurrencyMovementSequence.Snapshot start = CurrencyMovementIdStore.reconcileWithWorld(1L, 0L);
        assertEquals(1L, start.nextId());
        CurrencyMovementIdStore.recordAcceptedUpTo(217L);
        CurrencyMovementIdStore.recordNextId(CurrencyMovementIdStore.snapshot().nextId());
        assertEquals(218L, CurrencyMovementIdStore.snapshot().nextId());
        assertEquals(217L, CurrencyMovementIdStore.snapshot().acceptedUpTo());
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.readString(file).contains("\"accepted_up_to\": 217"));

        CurrencyMovementIdStore.resetForTests();
        CurrencyMovementIdStore.bind(file);
        CurrencyMovementSequence.Snapshot afterWipe = CurrencyMovementIdStore.reconcileWithWorld(1L, 0L);
        assertEquals(218L, afterWipe.nextId());
        assertEquals(217L, afterWipe.acceptedUpTo());
    }
}
