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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Grants LuckPerms groups for mapped FTB rank quests to the player who completed them.
 * FTB Quests progress is team-shared, so completion is attributed via
 * {@code ServerQuestFile.getCurrentPlayer()} and stored per player. Groups are never removed.
 *
 * <p>Login and sweep only replay already recorded per-player completions. They do not
 * infer ranks from team quest progress, grant teammates, or grant on team join.
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
    private static final QuestRankGrantCache CACHE = new QuestRankGrantCache();

    private static MinecraftServer server;
    private static int sweepTicks;
    private static int fullPassTicks;
    private static boolean pendingFullPass;

    private QuestRankService() {
    }

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        sweepTicks = 0;
        fullPassTicks = 0;
        pendingFullPass = true;
        CACHE.reset();
        QuestRankOnce.reset();
        if (minecraftServer == null) {
            return;
        }
        if (!QuestRankConfig.get().enabled()) {
            LOGGER.info("Quest-rank grants disabled by config");
            return;
        }
        if (!LuckPermsIntegration.isAvailable()) {
            LOGGER.info("Quest-rank grants inactive: LuckPerms is not loaded");
        }
        if (!FtbQuestProgress.isQuestsLoaded()) {
            LOGGER.info("Quest-rank grants inactive until FTB Quests is loaded");
        }
        try {
            FtbQuestCompletionHook.register();
        } catch (Throwable exception) {
            warnOnce("register-hook", exception);
        }
        LOGGER.info(
                "Quest-rank grants ready (mappings={}, sweep={}s, resetTeamProgress={})",
                QuestRankConfig.get().table().size(),
                QuestRankConfig.get().sweepIntervalSeconds(),
                QuestRankConfig.get().resetRankQuestTeamProgress()
        );
    }

    public static void onServerStarted(MinecraftServer minecraftServer) {
        try {
            server = minecraftServer;
            if (!ready()) {
                return;
            }
            sweep(minecraftServer, QuestRankTeamSyncLogic.REASON_SWEEP, true);
            pendingFullPass = false;
            fullPassTicks = 0;
        } catch (Throwable exception) {
            warnOnce("server-started", exception);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        try {
            if (!ready()) {
                return;
            }
            syncPlayer(player, QuestRankTeamSyncLogic.REASON_LOGIN);
        } catch (Throwable exception) {
            warnOnce(playerKey(player == null ? null : player.getUUID()), exception);
        }
    }

    public static void tick(MinecraftServer minecraftServer) {
        try {
            if (!ready()) {
                return;
            }
            int intervalTicks = QuestRankConfig.get().sweepIntervalTicks();
            sweepTicks++;
            fullPassTicks++;
            if (sweepTicks < intervalTicks) {
                return;
            }
            sweepTicks = 0;
            int fullPassIntervalTicks = QuestRankGrantCache.FULL_PASS_INTERVAL_SECONDS * 20;
            boolean fullPass = pendingFullPass || fullPassTicks >= fullPassIntervalTicks;
            sweep(minecraftServer, QuestRankTeamSyncLogic.REASON_SWEEP, fullPass);
            if (fullPass) {
                pendingFullPass = false;
                fullPassTicks = 0;
            }
        } catch (Throwable exception) {
            warnOnce("tick", exception);
        }
    }

    public static void onMappedQuestCompleted(
            long questId,
            UUID currentPlayerId,
            List<UUID> notifiedPlayers,
            List<UUID> onlineMembers,
            UUID teamId
    ) {
        try {
            if (!QuestRankConfig.get().enabled()) {
                return;
            }
            QuestRankTable table = QuestRankConfig.get().table();
            Optional<String> group = table.groupFor(questId);
            if (group.isEmpty()) {
                return;
            }
            Optional<UUID> playerId = QuestRankAttribution.completingPlayer(
                    currentPlayerId,
                    notifiedPlayers,
                    onlineMembers
            );
            if (playerId.isEmpty()) {
                LOGGER.debug(
                        "Quest-rank skip unattributed completion quest={}",
                        QuestRankIds.toHex(questId)
                );
                return;
            }
            MinecraftServer minecraftServer = server != null ? server : FtbQuestProgress.questServer();
            if (minecraftServer == null) {
                return;
            }
            QuestRankSavedData.get(minecraftServer).record(playerId.get(), questId);
            CACHE.invalidatePlayer(playerId.get());
            if (QuestRankConfig.get().resetRankQuestTeamProgress() && teamId != null) {
                try {
                    FtbQuestProgress.resetMappedQuest(teamId, questId, playerId.get());
                } catch (Throwable exception) {
                    warnOnce("reset:" + teamId, exception);
                }
            }
            grantRecorded(minecraftServer, playerId.get(), QuestRankTeamSyncLogic.REASON_QUEST_COMPLETE);
        } catch (Throwable exception) {
            warnOnce(playerKey(currentPlayerId), exception);
        }
    }

    public static SyncSummary sweep(MinecraftServer minecraftServer, String reason) {
        return sweep(minecraftServer, reason, true);
    }

    public static SyncSummary sweep(MinecraftServer minecraftServer, String reason, boolean fullPass) {
        try {
            if (minecraftServer == null || !ready()) {
                return remember(SyncSummary.unavailable(reason));
            }
            List<UUID> players = new ArrayList<>();
            for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
                if (player != null) {
                    players.add(player.getUUID());
                }
            }
            if (fullPass) {
                LOGGER.debug("Quest-rank full-pass sweep of {} online players", players.size());
            }
            return remember(grantPlayers(minecraftServer, players, reason));
        } catch (Throwable exception) {
            warnOnce("sweep", exception);
            return remember(SyncSummary.failed(reason));
        }
    }

    public static SyncSummary syncPlayer(ServerPlayer player, String reason) {
        try {
            if (player == null || !ready()) {
                return player == null ? SyncSummary.EMPTY : remember(SyncSummary.unavailable(reason));
            }
            return remember(grantRecorded(player.server, player.getUUID(), reason));
        } catch (Throwable exception) {
            UUID playerId = player == null ? null : player.getUUID();
            warnOnce(playerKey(playerId), exception);
            return remember(SyncSummary.failed(reason));
        }
    }

    public static SyncSummary syncPlayers(Collection<ServerPlayer> players, String reason) {
        try {
            if (!ready() || players == null || players.isEmpty()) {
                return SyncSummary.unavailable(reason);
            }
            MinecraftServer minecraftServer = serverOf(players);
            List<UUID> ids = new ArrayList<>();
            for (ServerPlayer player : players) {
                if (player != null) {
                    ids.add(player.getUUID());
                }
            }
            return remember(grantPlayers(minecraftServer, ids, reason));
        } catch (Throwable exception) {
            warnOnce("sync-players", exception);
            return remember(SyncSummary.failed(reason));
        }
    }

    private static SyncSummary grantPlayers(
            MinecraftServer minecraftServer,
            Collection<UUID> playerIds,
            String reason
    ) {
        if (minecraftServer == null || playerIds == null || playerIds.isEmpty()) {
            return new SyncSummary(reason, 0, 0, 0, 0, List.of(), "ok");
        }
        int granted = 0;
        int already = 0;
        int failed = 0;
        int scanned = 0;
        List<String> grantedNames = new ArrayList<>();
        for (UUID playerId : playerIds) {
            if (playerId == null) {
                continue;
            }
            try {
                SyncSummary part = grantRecorded(minecraftServer, playerId, reason);
                scanned += part.scanned();
                granted += part.granted();
                already += part.already();
                failed += part.failed();
                grantedNames.addAll(part.grantedNames());
            } catch (Throwable exception) {
                failed++;
                scanned++;
                warnOnce(playerKey(playerId), exception);
            }
        }
        SyncSummary summary = new SyncSummary(reason, scanned, granted, already, failed, grantedNames, "ok");
        LOGGER.debug("Quest-rank {} scanned={} granted={} already={}", reason, scanned, granted, already);
        return summary;
    }

    private static SyncSummary grantRecorded(MinecraftServer minecraftServer, UUID playerId, String callerReason) {
        QuestRankTable table = QuestRankConfig.get().table();
        if (minecraftServer == null || playerId == null || table.isEmpty()) {
            return new SyncSummary(callerReason, 0, 0, 0, 0, List.of(), table.isEmpty() ? "no mappings" : "ok");
        }
        Set<Long> recorded = mappedCompletions(QuestRankSavedData.get(minecraftServer).completed(playerId), table);
        int hash = QuestRankGrantCache.hashCompleted(recorded);
        String reason = QuestRankTeamSyncLogic.grantReason(callerReason);
        boolean cacheHit = CACHE.alreadyGranted(playerId, hash);
        int mapped = QuestRankGrantLogic.groupsToGrant(recorded, Set.of(), table).size();
        if (!QuestRankTeamSyncLogic.needsLuckPermsLoad(cacheHit, recorded, table)) {
            if (!cacheHit && mapped == 0) {
                CACHE.markGranted(playerId, hash);
            }
            return new SyncSummary(reason, 1, 0, mapped, 0, List.of(), "ok");
        }
        ServerPlayer onlinePlayer = minecraftServer.getPlayerList().getPlayer(playerId);
        boolean online = onlinePlayer != null;
        String fallbackName = online ? onlinePlayer.getGameProfile().getName() : "offline";
        if (online) {
            Set<String> held = LuckPermsUsers.inheritedGroupNames(playerId);
            List<String> missing = QuestRankGrantLogic.groupsToGrant(recorded, held, table);
            if (QuestRankTeamSyncLogic.shouldBackoff(reason)) {
                missing = QuestRankGrantBackoff.filter(playerId, missing);
            }
            int already = Math.max(0, mapped - missing.size());
            if (missing.isEmpty()) {
                CACHE.markGranted(playerId, hash);
                return new SyncSummary(reason, 1, 0, already, 0, List.of(), "ok");
            }
            scheduleGrant(minecraftServer, playerId, recorded, hash, table, reason, missing, fallbackName);
            return new SyncSummary(
                    reason,
                    1,
                    missing.size(),
                    already,
                    0,
                    List.of(fallbackName + "=" + String.join(",", missing)),
                    "ok"
            );
        }
        scheduleGrant(minecraftServer, playerId, recorded, hash, table, reason, null, fallbackName);
        return new SyncSummary(reason, 1, 0, 0, 0, List.of(), "ok");
    }

    private static Set<Long> mappedCompletions(Set<Long> recorded, QuestRankTable table) {
        if (recorded == null || recorded.isEmpty() || table == null || table.isEmpty()) {
            return Set.of();
        }
        Set<Long> mapped = new LinkedHashSet<>();
        for (long questId : recorded) {
            if (table.groupFor(questId).isPresent()) {
                mapped.add(questId);
            }
        }
        return Set.copyOf(mapped);
    }

    private static void scheduleGrant(
            MinecraftServer minecraftServer,
            UUID playerId,
            Set<Long> completed,
            int completedHash,
            QuestRankTable table,
            String reason,
            List<String> knownMissing,
            String fallbackName
    ) {
        LuckPermsUsers.loadUser(playerId).thenCompose(user -> {
            if (user == null) {
                return CompletableFuture.completedFuture(null);
            }
            Set<String> held = LuckPermsUsers.inheritedGroupNamesOf(user);
            List<String> missing = knownMissing != null
                    ? knownMissing
                    : QuestRankGrantLogic.groupsToGrant(completed, held, table);
            if (knownMissing == null && QuestRankTeamSyncLogic.shouldBackoff(reason)) {
                missing = QuestRankGrantBackoff.filter(playerId, missing);
            }
            if (missing.isEmpty()) {
                CACHE.markGranted(playerId, completedHash);
                cleanupIfOffline(minecraftServer, playerId, user);
                return CompletableFuture.completedFuture(null);
            }
            String name = LuckPermsUsers.loadedUsername(playerId).orElse(fallbackName);
            List<String> toGrant = missing;
            return LuckPermsUsers.addGroups(playerId, toGrant).whenComplete((ignored, error) -> {
                try {
                    finishGrant(playerId, name, toGrant, completed, table, reason, error);
                    if (error == null) {
                        CACHE.markGranted(playerId, completedHash);
                    }
                } finally {
                    cleanupIfOffline(minecraftServer, playerId, user);
                }
            });
        }).exceptionally(error -> {
            LOGGER.info(
                    "Quest rank grant failed: player={} uuid={} group=? quest=? reason={}: {}",
                    fallbackName,
                    playerId,
                    reason,
                    error.toString()
            );
            return null;
        });
    }

    private static void finishGrant(
            UUID playerId,
            String name,
            List<String> groups,
            Set<Long> completed,
            QuestRankTable table,
            String reason,
            Throwable error
    ) {
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
        Set<String> held = LuckPermsUsers.inheritedGroupNames(playerId);
        for (String group : groups) {
            if (held.contains(group)) {
                QuestRankGrantBackoff.clear(playerId, group);
                LOGGER.info(
                        "Quest rank granted: player={} uuid={} group={} quest={} reason={}",
                        name,
                        playerId,
                        group,
                        questHex(group, completed, table),
                        reason
                );
                continue;
            }
            if (QuestRankGrantBackoff.markMissingAfterWrite(playerId, group)) {
                LOGGER.warn(
                        "Quest rank grant wrote no inheritance node: player={} uuid={} group={} quest={} reason={}",
                        name,
                        playerId,
                        group,
                        questHex(group, completed, table),
                        reason
                );
            }
        }
    }

    private static void cleanupIfOffline(MinecraftServer minecraftServer, UUID playerId, Object user) {
        if (minecraftServer == null || playerId == null || user == null) {
            return;
        }
        if (minecraftServer.getPlayerList().getPlayer(playerId) == null) {
            LuckPermsUsers.cleanupUser(user);
        }
    }

    private static MinecraftServer serverOf(Collection<ServerPlayer> players) {
        if (server != null) {
            return server;
        }
        for (ServerPlayer player : players) {
            if (player != null && player.server != null) {
                return player.server;
            }
        }
        return FtbQuestProgress.questServer();
    }

    public static SyncSummary lastSummary() {
        return LAST.get();
    }

    public static String statusLine() {
        QuestRankSettings settings = QuestRankConfig.get();
        SyncSummary last = lastSummary();
        return "enabled=" + settings.enabled()
                + " mappings=" + settings.table().size()
                + " sweep=" + settings.sweepIntervalSeconds() + "s"
                + " resetTeamProgress=" + settings.resetRankQuestTeamProgress()
                + " ftbquests=" + (FtbQuestProgress.isQuestsLoaded() ? "yes" : "no")
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
                && !QuestRankConfig.get().table().isEmpty();
    }

    public static void warnOnce(String key, Throwable error) {
        if (!QuestRankOnce.first(key)) {
            return;
        }
        if (error == null) {
            LOGGER.warn("Quest-rank {} failed", key);
            return;
        }
        LOGGER.warn("Quest-rank {} failed: {}", key, error.toString(), error);
    }

    private static String playerKey(UUID playerId) {
        return "player:" + (playerId == null ? "unknown" : playerId);
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
            } else {
                note = "unavailable";
            }
            return new SyncSummary(reason, 0, 0, 0, 0, List.of(), note);
        }

        static SyncSummary failed(String reason) {
            return new SyncSummary(reason, 0, 0, 0, 1, List.of(), "error");
        }
    }
}
