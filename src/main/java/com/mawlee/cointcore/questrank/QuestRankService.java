package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.config.QuestRankConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Grants LuckPerms groups for completed FTB team quests. Online-only; offline players
 * are handled on login. Groups are never removed.
 *
 * <p>Prefix sync path: {@link LuckPermsUsers#addGroups} uses LuckPerms {@code modifyUser}
 * (console-level API, not a player command). That save fires {@code UserDataRecalculateEvent}.
 * {@link com.mawlee.cointcore.prefix.PrefixSelectionService} listens to that event and does
 * not suppress it here ({@code addGroups} does not call RecalcSuppressor), so site prefix
 * sync runs through {@code PrefixReportScheduler}.
 */
public final class QuestRankService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicReference<SyncSummary> LAST = new AtomicReference<>(SyncSummary.EMPTY);

    private static MinecraftServer server;
    private static int sweepTicks;

    private QuestRankService() {
    }

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        sweepTicks = 0;
        if (!QuestRankConfig.get().enabled()) {
            LOGGER.info("Quest-rank grants disabled by config");
            return;
        }
        if (!LuckPermsIntegration.isAvailable()) {
            LOGGER.info("Quest-rank grants inactive: LuckPerms is not loaded");
        }
        if (!FtbQuestProgress.isAvailable()) {
            LOGGER.info("Quest-rank grants inactive until FTB Quests and FTB Teams are loaded");
        }
        FtbQuestProgress.registerCompletionListener(members -> {
            MinecraftServer current = server;
            if (current == null) {
                return;
            }
            current.execute(() -> syncPlayers(members, "quest-complete"));
        });
        LOGGER.info(
                "Quest-rank grants ready (mappings={}, sweep={}s)",
                QuestRankConfig.get().table().size(),
                QuestRankConfig.get().sweepIntervalSeconds()
        );
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!ready()) {
            return;
        }
        syncPlayer(player, "login");
    }

    public static void tick(MinecraftServer minecraftServer) {
        if (!ready()) {
            return;
        }
        int intervalTicks = Math.max(5, QuestRankConfig.get().sweepIntervalSeconds()) * 20;
        sweepTicks++;
        if (sweepTicks < intervalTicks) {
            return;
        }
        sweepTicks = 0;
        sweep(minecraftServer, "sweep");
    }

    public static SyncSummary sweep(MinecraftServer minecraftServer, String reason) {
        if (minecraftServer == null || !ready()) {
            return remember(SyncSummary.unavailable(reason));
        }
        List<ServerPlayer> online = new ArrayList<>(minecraftServer.getPlayerList().getPlayers());
        return remember(syncPlayers(online, reason));
    }

    public static SyncSummary syncPlayer(ServerPlayer player, String reason) {
        if (player == null) {
            return SyncSummary.EMPTY;
        }
        return remember(syncPlayers(List.of(player), reason));
    }

    public static SyncSummary syncPlayers(Collection<ServerPlayer> players, String reason) {
        if (!ready() || players == null || players.isEmpty()) {
            return SyncSummary.unavailable(reason);
        }
        QuestRankTable table = QuestRankConfig.get().table();
        if (table.isEmpty()) {
            return remember(new SyncSummary(reason, 0, 0, 0, 0, List.of(), "no mappings"));
        }
        Map<UUID, Set<Long>> completedByTeam = new HashMap<>();
        int granted = 0;
        int already = 0;
        int failed = 0;
        int scanned = 0;
        List<String> grantedNames = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            scanned++;
            UUID teamKey = FtbQuestProgress.teamId(player).orElse(player.getUUID());
            Set<Long> completed = completedByTeam.computeIfAbsent(
                    teamKey,
                    ignored -> FtbQuestProgress.completedMapped(player, table.questIds())
            );
            Set<String> held = LuckPermsUsers.inheritedGroupNames(player.getUUID());
            List<String> missing = QuestRankGrantLogic.groupsToGrant(completed, held, table);
            already += Math.max(0, completed.size() - missing.size());
            if (missing.isEmpty()) {
                continue;
            }
            grant(player, missing, completed, table, reason);
            granted += missing.size();
            grantedNames.add(player.getGameProfile().getName() + "=" + String.join(",", missing));
        }
        SyncSummary summary = new SyncSummary(reason, scanned, granted, already, failed, grantedNames, "ok");
        LOGGER.debug("Quest-rank {} scanned={} granted={} already={}", reason, scanned, granted, already);
        return remember(summary);
    }

    public static SyncSummary lastSummary() {
        return LAST.get();
    }

    public static String statusLine() {
        QuestRankConfig.Settings settings = QuestRankConfig.get();
        SyncSummary last = lastSummary();
        return "enabled=" + settings.enabled()
                + " mappings=" + settings.table().size()
                + " sweep=" + settings.sweepIntervalSeconds() + "s"
                + " ftbquests=" + (FtbQuestProgress.isQuestsLoaded() ? "yes" : "no")
                + " ftbteams=" + (FtbQuestProgress.isTeamsLoaded() ? "yes" : "no")
                + " luckperms=" + (LuckPermsIntegration.isAvailable() ? "yes" : "no")
                + " last=[" + last.reason()
                + " scanned=" + last.scanned()
                + " granted=" + last.granted()
                + " already=" + last.already()
                + "]";
    }

    public static boolean ready() {
        return QuestRankConfig.get().enabled()
                && LuckPermsIntegration.isAvailable()
                && FtbQuestProgress.isAvailable()
                && !QuestRankConfig.get().table().isEmpty();
    }

    private static void grant(
            ServerPlayer player,
            List<String> groups,
            Set<Long> completed,
            QuestRankTable table,
            String reason
    ) {
        UUID playerId = player.getUUID();
        String name = player.getGameProfile().getName();
        LuckPermsUsers.addGroups(playerId, groups).whenComplete((ignored, error) -> {
            if (error != null) {
                for (String group : groups) {
                    LOGGER.info(
                            "Quest rank grant failed: player={} uuid={} group={} quest={} reason={}: {}",
                            name,
                            playerId,
                            group,
                            questHex(group, completed, table),
                            reason,
                            error.toString()
                    );
                }
                return;
            }
            for (String group : groups) {
                LOGGER.info(
                        "Quest rank granted: player={} uuid={} group={} quest={} reason={}",
                        name,
                        playerId,
                        group,
                        questHex(group, completed, table),
                        reason
                );
            }
        });
    }

    private static String questHex(String group, Set<Long> completed, QuestRankTable table) {
        for (var entry : table.questToGroup().entrySet()) {
            if (group.equals(entry.getValue()) && completed.contains(entry.getKey())) {
                return QuestRankIds.toHex(entry.getKey());
            }
        }
        return "?";
    }

    private static SyncSummary remember(SyncSummary summary) {
        LAST.set(summary);
        return summary;
    }

    public record SyncSummary(
            String reason,
            int scanned,
            int granted,
            int already,
            int failed,
            List<String> grantedNames,
            String note
    ) {
        static final SyncSummary EMPTY = new SyncSummary("none", 0, 0, 0, 0, List.of(), "none");

        public SyncSummary {
            reason = reason == null ? "" : reason;
            grantedNames = grantedNames == null ? List.of() : List.copyOf(grantedNames);
            note = note == null ? "" : note;
        }

        static SyncSummary unavailable(String reason) {
            String note;
            if (!QuestRankConfig.get().enabled()) {
                note = "disabled";
            } else if (!LuckPermsIntegration.isAvailable()) {
                note = "luckperms missing";
            } else if (!FtbQuestProgress.isAvailable()) {
                note = "ftbquests/ftbteams missing";
            } else {
                note = "unavailable";
            }
            return new SyncSummary(reason, 0, 0, 0, 0, List.of(), note);
        }
    }
}
