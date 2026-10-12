package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.config.QuestRankConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Grants LuckPerms groups for completed FTB team quests to every team member,
 * including offline players. Players who later join a team with completed rank
 * quests are granted on the next sweep (uuid→teamId change) or immediately via
 * {@link FtbTeamJoinHook}. Groups are never removed.
 *
 * <p>There is no FTB Quests completion-event hook (Architectury EventActor
 * mismatch crashed the server in #49).
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
        if (!FtbQuestProgress.isAvailable()) {
            LOGGER.info("Quest-rank grants inactive until FTB Quests and FTB Teams are loaded");
        }
        if (FtbQuestProgress.isTeamsLoaded()) {
            FtbTeamJoinHook.register();
        }
        LOGGER.info(
                "Quest-rank grants ready (mappings={}, sweep={}s, full-pass={}s)",
                QuestRankConfig.get().table().size(),
                QuestRankConfig.get().sweepIntervalSeconds(),
                QuestRankGrantCache.FULL_PASS_INTERVAL_SECONDS
        );
    }

    public static void onServerStarted(MinecraftServer minecraftServer) {
        server = minecraftServer;
        if (!ready()) {
            return;
        }
        sweep(minecraftServer, QuestRankTeamSyncLogic.REASON_SWEEP, true);
        pendingFullPass = false;
        fullPassTicks = 0;
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!ready()) {
            return;
        }
        syncPlayer(player, QuestRankTeamSyncLogic.REASON_LOGIN);
    }

    public static void tick(MinecraftServer minecraftServer) {
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
    }

    public static SyncSummary sweep(MinecraftServer minecraftServer, String reason) {
        return sweep(minecraftServer, reason, true);
    }

    public static SyncSummary sweep(MinecraftServer minecraftServer, String reason, boolean fullPass) {
        if (minecraftServer == null || !ready()) {
            return remember(SyncSummary.unavailable(reason));
        }
        return remember(syncTeams(minecraftServer, collectTeams(minecraftServer, fullPass), reason));
    }

    public static SyncSummary syncPlayer(ServerPlayer player, String reason) {
        if (player == null || !ready()) {
            return player == null ? SyncSummary.EMPTY : remember(SyncSummary.unavailable(reason));
        }
        Optional<Team> team = FtbQuestProgress.teamOf(player);
        if (team.isEmpty()) {
            return remember(syncPlayers(List.of(player), reason));
        }
        return remember(syncTeamMembers(
                player.server,
                team.get(),
                Set.of(player.getUUID()),
                reason
        ));
    }

    public static SyncSummary syncTeam(MinecraftServer minecraftServer, Team team, String reason) {
        if (minecraftServer == null || team == null || !ready()) {
            return remember(SyncSummary.unavailable(reason));
        }
        return remember(syncTeams(minecraftServer, List.of(team), reason));
    }

    public static SyncSummary syncPlayers(Collection<ServerPlayer> players, String reason) {
        if (!ready() || players == null || players.isEmpty()) {
            return SyncSummary.unavailable(reason);
        }
        Map<UUID, Team> teams = new LinkedHashMap<>();
        List<ServerPlayer> withoutTeam = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            Optional<Team> team = FtbQuestProgress.teamOf(player);
            if (team.isPresent()) {
                teams.put(team.get().getId(), team.get());
            } else {
                withoutTeam.add(player);
            }
        }
        SyncSummary teamSummary = teams.isEmpty()
                ? SyncSummary.EMPTY
                : syncTeams(serverOf(players), teams.values(), reason);
        if (withoutTeam.isEmpty()) {
            return teamSummary;
        }
        return merge(teamSummary, grantOnlineWithoutTeam(withoutTeam, reason));
    }

    private static Collection<Team> collectTeams(MinecraftServer minecraftServer, boolean fullPass) {
        Map<UUID, Team> teams = new LinkedHashMap<>();
        if (fullPass && FtbQuestProgress.isManagerLoaded()) {
            for (Team team : FtbQuestProgress.allTeams()) {
                if (team != null) {
                    teams.put(team.getId(), team);
                }
            }
        }
        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            FtbQuestProgress.teamOf(player).ifPresent(team -> teams.put(team.getId(), team));
        }
        return teams.values();
    }

    private static SyncSummary syncTeams(
            MinecraftServer minecraftServer,
            Collection<Team> teams,
            String reason
    ) {
        if (minecraftServer == null || teams == null || teams.isEmpty()) {
            return new SyncSummary(reason, 0, 0, 0, 0, List.of(), "ok");
        }
        int granted = 0;
        int already = 0;
        int failed = 0;
        int scanned = 0;
        List<String> grantedNames = new ArrayList<>();
        for (Team team : teams) {
            if (team == null) {
                continue;
            }
            SyncSummary part = syncTeamMembers(minecraftServer, team, team.getMembers(), reason);
            scanned += part.scanned();
            granted += part.granted();
            already += part.already();
            failed += part.failed();
            grantedNames.addAll(part.grantedNames());
        }
        SyncSummary summary = new SyncSummary(reason, scanned, granted, already, failed, grantedNames, "ok");
        LOGGER.debug("Quest-rank {} scanned={} granted={} already={}", reason, scanned, granted, already);
        return summary;
    }

    private static SyncSummary syncTeamMembers(
            MinecraftServer minecraftServer,
            Team team,
            Collection<UUID> members,
            String callerReason
    ) {
        QuestRankTable table = QuestRankConfig.get().table();
        if (table.isEmpty() || members == null || members.isEmpty()) {
            return new SyncSummary(callerReason, 0, 0, 0, 0, List.of(), table.isEmpty() ? "no mappings" : "ok");
        }
        UUID teamId = team.getId();
        Set<Long> completed = FtbQuestProgress.completedMapped(teamId, table.questIds());
        int completedHash = QuestRankGrantCache.hashCompleted(completed);
        Set<UUID> memberSet = Set.copyOf(members);
        CACHE.updateTeamState(teamId, memberSet, completedHash);

        int granted = 0;
        int already = 0;
        int failed = 0;
        int scanned = 0;
        List<String> grantedNames = new ArrayList<>();
        for (UUID memberId : memberSet) {
            if (memberId == null) {
                continue;
            }
            scanned++;
            boolean teamChanged = CACHE.trackTeam(memberId, teamId).isPresent();
            boolean online = minecraftServer.getPlayerList().getPlayer(memberId) != null;
            String reason = QuestRankTeamSyncLogic.grantReason(teamChanged, online, callerReason);
            boolean cacheHit = CACHE.alreadyGranted(memberId, teamId, completedHash);
            int mapped = QuestRankGrantLogic.groupsToGrant(completed, Set.of(), table).size();
            if (!QuestRankTeamSyncLogic.needsLuckPermsLoad(cacheHit, completed, table)) {
                already += mapped;
                if (!cacheHit) {
                    CACHE.markGranted(memberId, teamId, completedHash);
                }
                continue;
            }
            MemberGrant result = ensureGroups(
                    minecraftServer,
                    memberId,
                    teamId,
                    completed,
                    completedHash,
                    table,
                    reason,
                    mapped
            );
            granted += result.granted();
            already += result.already();
            failed += result.failed();
            if (!result.label().isEmpty()) {
                grantedNames.add(result.label());
            }
        }
        return new SyncSummary(callerReason, scanned, granted, already, failed, grantedNames, "ok");
    }

    private static SyncSummary grantOnlineWithoutTeam(Collection<ServerPlayer> players, String reason) {
        QuestRankTable table = QuestRankConfig.get().table();
        int granted = 0;
        int already = 0;
        int failed = 0;
        int scanned = 0;
        List<String> grantedNames = new ArrayList<>();
        for (ServerPlayer player : players) {
            scanned++;
            UUID playerId = player.getUUID();
            Set<Long> completed = FtbQuestProgress.completedMapped(player, table.questIds());
            int hash = QuestRankGrantCache.hashCompleted(completed);
            boolean cacheHit = CACHE.alreadyGranted(playerId, playerId, hash);
            int mapped = QuestRankGrantLogic.groupsToGrant(completed, Set.of(), table).size();
            if (!QuestRankTeamSyncLogic.needsLuckPermsLoad(cacheHit, completed, table)) {
                already += mapped;
                if (!cacheHit) {
                    CACHE.markGranted(playerId, playerId, hash);
                }
                continue;
            }
            MemberGrant result = ensureGroups(
                    player.server,
                    playerId,
                    playerId,
                    completed,
                    hash,
                    table,
                    reason,
                    mapped
            );
            granted += result.granted();
            already += result.already();
            failed += result.failed();
            if (!result.label().isEmpty()) {
                grantedNames.add(result.label());
            }
        }
        return new SyncSummary(reason, scanned, granted, already, failed, grantedNames, "ok");
    }

    private static MemberGrant ensureGroups(
            MinecraftServer minecraftServer,
            UUID playerId,
            UUID teamId,
            Set<Long> completed,
            int completedHash,
            QuestRankTable table,
            String reason,
            int mapped
    ) {
        ServerPlayer onlinePlayer = minecraftServer.getPlayerList().getPlayer(playerId);
        boolean online = onlinePlayer != null;
        String fallbackName = online ? onlinePlayer.getGameProfile().getName() : "offline";
        if (online) {
            Set<String> held = LuckPermsUsers.inheritedGroupNames(playerId);
            List<String> missing = QuestRankGrantLogic.groupsToGrant(completed, held, table);
            if (QuestRankTeamSyncLogic.shouldBackoff(reason)) {
                missing = QuestRankGrantBackoff.filter(playerId, missing);
            }
            int already = Math.max(0, mapped - missing.size());
            if (missing.isEmpty()) {
                CACHE.markGranted(playerId, teamId, completedHash);
                return new MemberGrant(0, already, 0, "");
            }
            scheduleGrant(minecraftServer, playerId, teamId, completed, completedHash, table, reason, missing, fallbackName);
            return new MemberGrant(missing.size(), already, 0, fallbackName + "=" + String.join(",", missing));
        }
        scheduleGrant(minecraftServer, playerId, teamId, completed, completedHash, table, reason, null, fallbackName);
        return new MemberGrant(0, 0, 0, "");
    }

    private static void scheduleGrant(
            MinecraftServer minecraftServer,
            UUID playerId,
            UUID teamId,
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
                CACHE.markGranted(playerId, teamId, completedHash);
                cleanupIfOffline(minecraftServer, playerId, user);
                return CompletableFuture.completedFuture(null);
            }
            String name = LuckPermsUsers.loadedUsername(playerId).orElse(fallbackName);
            List<String> toGrant = missing;
            return LuckPermsUsers.addGroups(playerId, toGrant).whenComplete((ignored, error) -> {
                try {
                    finishGrant(playerId, name, toGrant, completed, table, reason, error);
                    if (error == null) {
                        CACHE.markGranted(playerId, teamId, completedHash);
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
        return null;
    }

    private static SyncSummary merge(SyncSummary left, SyncSummary right) {
        List<String> names = new ArrayList<>(left.grantedNames());
        names.addAll(right.grantedNames());
        return new SyncSummary(
                left.reason().isEmpty() ? right.reason() : left.reason(),
                left.scanned() + right.scanned(),
                left.granted() + right.granted(),
                left.already() + right.already(),
                left.failed() + right.failed(),
                names,
                "ok"
        );
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

    private record MemberGrant(int granted, int already, int failed, String label) {
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
