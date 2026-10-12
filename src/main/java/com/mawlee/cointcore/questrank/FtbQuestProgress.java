package com.mawlee.cointcore.questrank;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Typed FTB Quests 2101.1.36 access. Rank grants do not read team completion.
 *
 * <p>{@link ServerQuestFile} signatures used here:
 * <ul>
 *   <li>{@code getNullableTeamData(UUID)} / {@code getOrCreateTeamData(UUID)} — team id</li>
 *   <li>{@code getOrCreateTeamData(Team)} — FTB Team, not UUID</li>
 *   <li>{@code getTeamData(Player)} — Optional, player only; there is no {@code getTeamData(UUID)}</li>
 *   <li>{@code getCurrentPlayer()} / {@code withPlayerContext} — player who submitted the task</li>
 * </ul>
 */
public final class FtbQuestProgress {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FtbQuestProgress() {
    }

    public static boolean isQuestsLoaded() {
        return ModList.get().isLoaded("ftbquests");
    }

    public static boolean isTeamsLoaded() {
        return ModList.get().isLoaded("ftbteams");
    }

    public static boolean isAvailable() {
        return isQuestsLoaded() && isTeamsLoaded();
    }

    public static boolean isServerSideFile() {
        ServerQuestFile file = serverFile();
        return file != null && file.isServerSide();
    }

    public static MinecraftServer questServer() {
        ServerQuestFile file = serverFile();
        return file == null ? null : file.server;
    }

    public static UUID currentPlayerId() {
        ServerPlayer player = currentPlayer();
        return player == null ? null : player.getUUID();
    }

    public static ServerPlayer currentPlayer() {
        ServerQuestFile file = serverFile();
        if (file == null) {
            return null;
        }
        try {
            return file.getCurrentPlayer();
        } catch (RuntimeException exception) {
            LOGGER.debug("ServerQuestFile.getCurrentPlayer failed", exception);
            return null;
        }
    }

    /**
     * Resets one mapped rank quest (and its tasks) on the team so a teammate can complete it.
     * Uses {@link Quest#forceProgress} plus {@link TeamData#setProgress}(task, 0) so leftover
     * task progress cannot auto-complete the quest again on login.
     */
    public static boolean resetMappedQuest(UUID teamId, long questId, UUID playerId) {
        ServerQuestFile file = serverFile();
        if (file == null || teamId == null) {
            return false;
        }
        try {
            TeamData data = teamData(file, teamId);
            if (data == null) {
                return false;
            }
            Quest quest = file.getQuest(questId);
            if (quest == null) {
                return false;
            }
            UUID actor = playerId == null ? Util.NIL_UUID : playerId;
            ProgressChange change = new ProgressChange(quest, actor);
            change.setReset(true);
            quest.forceProgress(data, change);
            Collection<?> tasks = quest.getTasks();
            if (tasks != null) {
                for (Object taskObj : tasks) {
                    if (taskObj instanceof Task task) {
                        data.setProgress(task, 0L);
                    }
                }
            }
            return true;
        } catch (RuntimeException exception) {
            LOGGER.debug("Failed to reset mapped rank quest {} for team {}", QuestRankIds.toHex(questId), teamId, exception);
            return false;
        }
    }

    public static List<UUID> playerIds(Collection<?> players) {
        if (players == null || players.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (Object value : players) {
            if (value instanceof Player player && !ids.contains(player.getUUID())) {
                ids.add(player.getUUID());
            }
        }
        return List.copyOf(ids);
    }

    /**
     * Team progress lookup uses UUID overloads only. Never {@code getTeamData(UUID)}:
     * that method takes {@link Player} and a UUID argument crashes with
     * {@code IllegalArgumentException: argument type mismatch}.
     */
    static TeamData teamData(ServerQuestFile file, UUID teamId) {
        if (file == null || teamId == null) {
            return null;
        }
        TeamData existing = file.getNullableTeamData(teamId);
        if (existing != null) {
            return existing;
        }
        return file.getOrCreateTeamData(teamId);
    }

    private static ServerQuestFile serverFile() {
        if (!isQuestsLoaded()) {
            return null;
        }
        try {
            return ServerQuestFile.INSTANCE;
        } catch (Throwable exception) {
            LOGGER.debug("ServerQuestFile.INSTANCE unavailable", exception);
            return null;
        }
    }
}
