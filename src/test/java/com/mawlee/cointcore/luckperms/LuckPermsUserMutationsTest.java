package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckPermsUserMutationsTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @AfterEach
    void tearDown() {
        LuckPermsUserMutations.reset();
    }

    @Test
    void serializesGrantThenPrefixForSamePlayer() throws Exception {
        List<String> order = new ArrayList<>();
        CompletableFuture<Void> grantGate = new CompletableFuture<>();
        CompletableFuture<Void> grant = LuckPermsUserMutations.enqueue(PLAYER, () -> grantGate.thenRun(() ->
                order.add("grant")));
        CompletableFuture<Void> prefix = LuckPermsUserMutations.enqueue(PLAYER, () -> {
            order.add("prefix");
            return CompletableFuture.completedFuture(null);
        });

        assertEquals(List.of(), order);
        grantGate.complete(null);
        grant.get(2, TimeUnit.SECONDS);
        prefix.get(2, TimeUnit.SECONDS);
        assertEquals(List.of("grant", "prefix"), order);
        assertEquals(0, LuckPermsUserMutations.pendingTails());
    }

    @Test
    void differentPlayersDoNotBlockEachOther() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Void> first = LuckPermsUserMutations.enqueue(PLAYER, () -> CompletableFuture.runAsync(() -> {
            firstStarted.countDown();
            try {
                assertTrue(releaseFirst.await(2, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
        CompletableFuture<Void> second = LuckPermsUserMutations.enqueue(OTHER, () ->
                CompletableFuture.completedFuture(null));
        second.get(2, TimeUnit.SECONDS);
        releaseFirst.countDown();
        first.get(2, TimeUnit.SECONDS);
    }
}
