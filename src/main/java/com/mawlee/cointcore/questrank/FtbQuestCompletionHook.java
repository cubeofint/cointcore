package com.mawlee.cointcore.questrank;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventActor;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Architectury {@code Event<EventActor<ObjectCompletedEvent.QuestEvent>>} listener.
 *
 * <p>Must be a real {@link EventActor} lambda compiled against the Architectury API.
 * A {@code java.lang.reflect.Proxy} is not an {@code EventActor} and ClassCastException-crashes
 * {@code Quest.onCompleted} (see #49).
 */
public final class FtbQuestCompletionHook {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private FtbQuestCompletionHook() {
    }

    public static void register() {
        if (registered || !ModList.get().isLoaded("ftbquests")) {
            return;
        }
        try {
            EventActor<ObjectCompletedEvent.QuestEvent> listener = event -> {
                try {
                    handle(event);
                } catch (Throwable exception) {
                    QuestRankService.warnOnce("completion-event", exception);
                }
                return EventResult.pass();
            };
            ObjectCompletedEvent.QUEST.register(listener);
            registered = true;
            LOGGER.info("Quest-rank ObjectCompletedEvent.QUEST EventActor registered");
        } catch (Throwable exception) {
            LOGGER.warn("Quest-rank completion hook failed to register: {}", exception.toString());
        }
    }

    private static void handle(ObjectCompletedEvent.QuestEvent event) {
        if (event == null || !FtbQuestProgress.isServerSideFile()) {
            return;
        }
        Quest quest = event.getQuest();
        if (quest == null) {
            return;
        }
        TeamData teamData = event.getData();
        UUID teamId = teamData == null ? null : teamData.getTeamId();
        UUID currentPlayerId = FtbQuestProgress.currentPlayerId();
        List<UUID> notified = FtbQuestProgress.playerIds(event.getNotifiedPlayers());
        List<UUID> online = FtbQuestProgress.playerIds(event.getOnlineMembers());
        QuestRankService.onMappedQuestCompleted(quest.id, currentPlayerId, notified, online, teamId);
    }
}
