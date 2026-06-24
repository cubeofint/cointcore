package com.mawlee.cointcore.claim;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local stack of acting player UUIDs for Create assembly / contraption chains.
 */
public final class ClaimGuardContext {
    private static final ThreadLocal<Deque<UUID>> ACTOR_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<UUID>> INTERACTOR_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private ClaimGuardContext() {
    }

    public static void pushActor(UUID playerId) {
        if (playerId != null) {
            ACTOR_STACK.get().push(playerId);
        }
    }

    public static void popActor() {
        Deque<UUID> stack = ACTOR_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static Optional<UUID> currentActor() {
        Deque<UUID> stack = ACTOR_STACK.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }

    public static void runWithActor(UUID playerId, Runnable action) {
        pushActor(playerId);
        try {
            action.run();
        } finally {
            popActor();
        }
    }

    public static void clear() {
        ACTOR_STACK.get().clear();
        INTERACTOR_STACK.get().clear();
    }

    public static void pushInteractor(UUID playerId) {
        if (playerId != null) {
            INTERACTOR_STACK.get().push(playerId);
        }
    }

    public static void popInteractor() {
        Deque<UUID> stack = INTERACTOR_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static Optional<UUID> currentInteractor() {
        Deque<UUID> stack = INTERACTOR_STACK.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }
}
