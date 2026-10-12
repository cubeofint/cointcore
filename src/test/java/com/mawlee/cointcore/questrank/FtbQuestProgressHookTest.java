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
 * {@code Consumer} is not an {@code EventActor} and crashes FTB Quests
 * {@code Quest.onCompleted}. Rank grants register a real EventActor lambda.
 */
class FtbQuestProgressHookTest {
    private static final Path PROGRESS =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/FtbQuestProgress.java");
    private static final Path SERVICE =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/QuestRankService.java");
    private static final Path HOOK =
            Path.of("src/main/java/com/mawlee/cointcore/questrank/FtbQuestCompletionHook.java");

    @Test
    void completionHookIsRealEventActorLambdaNotProxy() throws IOException {
        String progress = Files.readString(PROGRESS);
        String service = Files.readString(SERVICE);
        String hook = Files.readString(HOOK);

        assertFalse(progress.contains("Proxy.newProxyInstance"));
        assertFalse(progress.contains("import java.lang.reflect.Proxy"));
        assertFalse(hook.contains("Proxy.newProxyInstance"));
        assertFalse(hook.contains("import java.lang.reflect.Proxy"));
        assertTrue(hook.contains("EventActor<ObjectCompletedEvent.QuestEvent>"));
        assertTrue(hook.contains("dev.architectury.event.EventActor"));
        assertTrue(hook.contains("EventResult.pass()"));
        assertTrue(hook.contains("ObjectCompletedEvent.QUEST.register(listener)"));
        assertTrue(service.contains("onMappedQuestCompleted"));
        assertTrue(service.contains("onPlayerJoin"));
        assertTrue(service.contains("sweep"));
        assertFalse(service.contains("syncTeam"));
        assertFalse(service.contains("FtbTeamJoinHook"));
        assertFalse(Files.exists(Path.of("src/main/java/com/mawlee/cointcore/questrank/FtbTeamJoinHook.java")));
    }

    @Test
    void teamDataLookupUsesUuidOverloadsNotGetTeamDataUuid() throws IOException {
        String progress = Files.readString(PROGRESS);
        assertTrue(progress.contains("getNullableTeamData(teamId)"));
        assertTrue(progress.contains("getOrCreateTeamData(teamId)"));
        assertTrue(progress.contains("getCurrentPlayer"));
        assertFalse(progress.contains("getTeamData(teamId)"));
        assertFalse(progress.contains("completedMapped"));
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
    void realEventActorLambdaReturnsPass() {
        EventActor<Object> listener = ignored -> EventResult.pass();
        assertEquals(EventResult.PASS, invokeAsEventActor(listener));
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
