package com.mawlee.cointcore.questrank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Picks the one player who completed a mapped rank quest.
 *
 * <p>FTB Quests progress is team-shared. {@code ObjectCompletedEvent} lists every online
 * member in {@code notifiedPlayers}/{@code onlineMembers}, so those lists are not a grant
 * target. The reliable handle in 2101.1.36 is {@code ServerQuestFile.getCurrentPlayer()},
 * set by {@code withPlayerContext} around {@code Task.submitTask}.
 */
public final class QuestRankAttribution {
    private QuestRankAttribution() {
    }

    /**
     * @param currentPlayerId {@link dev.ftb.mods.ftbquests.quest.ServerQuestFile#getCurrentPlayer()}
     * @param notifiedPlayers event notified list (usually every online teammate, or empty)
     * @param onlineMembers event online team members
     */
    public static Optional<UUID> completingPlayer(
            UUID currentPlayerId,
            List<UUID> notifiedPlayers,
            List<UUID> onlineMembers
    ) {
        if (currentPlayerId != null) {
            return Optional.of(currentPlayerId);
        }
        Optional<UUID> onlyNotified = onlyElement(notifiedPlayers);
        if (onlyNotified.isPresent()) {
            return onlyNotified;
        }
        return onlyElement(onlineMembers);
    }

    private static Optional<UUID> onlyElement(List<UUID> ids) {
        if (ids == null || ids.size() != 1) {
            return Optional.empty();
        }
        UUID id = ids.getFirst();
        return id == null ? Optional.empty() : Optional.of(id);
    }
}
