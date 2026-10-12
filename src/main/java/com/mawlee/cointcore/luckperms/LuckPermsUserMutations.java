package com.mawlee.cointcore.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Serializes LuckPerms user mutations per UUID so grant {@code modifyUser}
 * and prefix {@code saveUser} cannot interleave and drop meta.
 */
public final class LuckPermsUserMutations {
    private static final ConcurrentHashMap<UUID, CompletableFuture<?>> TAILS = new ConcurrentHashMap<>();

    private LuckPermsUserMutations() {
    }

    public static <T> CompletableFuture<T> enqueue(UUID playerId, Supplier<CompletableFuture<T>> work) {
        if (work == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("work"));
        }
        if (playerId == null) {
            return safeGet(work);
        }
        CompletableFuture<T> done = new CompletableFuture<>();
        TAILS.compute(playerId, (id, previous) -> {
            CompletableFuture<?> wait = previous == null ? CompletableFuture.completedFuture(null) : previous;
            wait.whenComplete((ignored, error) -> run(work, done));
            return done;
        });
        done.whenComplete((ignored, error) -> TAILS.remove(playerId, done));
        return done;
    }

    static void reset() {
        TAILS.clear();
    }

    static int pendingTails() {
        return TAILS.size();
    }

    private static <T> void run(Supplier<CompletableFuture<T>> work, CompletableFuture<T> done) {
        try {
            CompletableFuture<T> future = work.get();
            if (future == null) {
                done.complete(null);
                return;
            }
            future.whenComplete((value, error) -> {
                if (error != null) {
                    done.completeExceptionally(error);
                } else {
                    done.complete(value);
                }
            });
        } catch (RuntimeException exception) {
            done.completeExceptionally(exception);
        }
    }

    private static <T> CompletableFuture<T> safeGet(Supplier<CompletableFuture<T>> work) {
        try {
            CompletableFuture<T> future = work.get();
            return future == null ? CompletableFuture.completedFuture(null) : future;
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
