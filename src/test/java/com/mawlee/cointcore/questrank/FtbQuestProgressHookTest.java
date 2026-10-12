package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architectury {@code Event&lt;EventActor&lt;T&gt;&gt;} casts each listener to
 * {@code EventActor} (see EventFactory.handleInvocation). A {@code Proxy} of
 * {@code Consumer} or the event's inner type is not an {@code EventActor} and
 * crashes FTB Quests {@code Quest.onCompleted}. Quest ranks therefore do not
 * register on {@code ObjectCompletedEvent}; login + sweep grant ranks instead.
 */
class FtbQuestProgressHookTest {
    private static final Path PROGRESS =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/FtbQuestProgress.java");
    private static final Path SERVICE =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/QuestRankService.java");
    private static final Path JOIN_HOOK =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/FtbTeamJoinHook.java");

    @Test
    void questRankSourcesDoNotRegisterArchitecturyCompletionProxy() throws IOException {
        String progress = Files.readString(PROGRESS);
        String service = Files.readString(SERVICE);
        String joinHook = Files.readString(JOIN_HOOK);

        assertFalse(progress.contains("Proxy.newProxyInstance"));
        assertFalse(progress.contains("import java.lang.reflect.Proxy"));
        assertFalse(progress.contains("registerCompletionListener"));
        assertFalse(progress.contains("subscribeCompletedEvent"));
        assertFalse(service.contains("registerCompletionListener"));
        assertFalse(service.contains("quest-complete"));
        assertFalse(joinHook.contains("Proxy.newProxyInstance"));
        assertFalse(joinHook.contains("import java.lang.reflect.Proxy"));
        assertTrue(progress.contains("completedMapped"));
        assertTrue(service.contains("onPlayerJoin"));
        assertTrue(service.contains("sweep"));
        assertTrue(joinHook.contains("PLAYER_JOINED_PARTY"));
        assertTrue(joinHook.contains("FtbTeamJoinHook::onPlayerJoinedParty"));
        assertTrue(joinHook.contains("server.execute"));
        assertTrue(joinHook.contains("QuestRankService.syncTeam"));
        assertTrue(joinHook.contains("Consumer<PlayerJoinedPartyTeamEvent>"));
    }

    @Test
    void consumerProxyCannotBeCastToEventActor() {
        Object proxy = Proxy.newProxyInstance(
                EventActor.class.getClassLoader(),
                new Class<?>[]{Consumer.class},
                (ignored, method, args) -> defaultReturn(method.getReturnType())
        );

        ClassCastException thrown = assertThrows(
                ClassCastException.class,
                () -> invokeAsEventActor(proxy)
        );
        assertTrue(thrown.getMessage().contains("EventActor") || thrown.getMessage().contains("Proxy"));
    }

    @Test
    void eventActorProxyActReturnsPassAndDoesNotThrow() {
        Object proxy = Proxy.newProxyInstance(
                EventActor.class.getClassLoader(),
                new Class<?>[]{EventActor.class},
                (ignored, method, args) -> {
                    try {
                        if ("act".equals(method.getName())) {
                            return EventResult.pass();
                        }
                        return defaultReturn(method.getReturnType());
                    } catch (RuntimeException exception) {
                        return EventResult.pass();
                    }
                }
        );

        assertEquals(EventResult.PASS, invokeAsEventActor(proxy));
    }

    @SuppressWarnings("unchecked")
    private static EventResult invokeAsEventActor(Object listener) {
        return ((EventActor<Object>) listener).act(new Object());
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.TRUE;
        }
        return null;
    }

    interface EventActor<T> {
        EventResult act(T event);
    }

    enum EventResult {
        PASS;

        static EventResult pass() {
            return PASS;
        }
    }
}
