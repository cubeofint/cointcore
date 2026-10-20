package com.mawlee.cointcore.ftb;

import com.mawlee.cointcore.config.ForceLoadIdleConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Releases FTB Chunks force-loads of teams idle for {@link ForceLoadIdleConfig#getIdleHours()} unless a member
 * holds {@link CointPermissionNodes#FORCELOAD_EXTENDED}. Extended teams fall back to FTB's own
 * {@code max_idle_days_before_unforce} (set it to the donor window, e.g. 3 days).
 */
public final class ForceLoadIdleService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long HOUR_MS = 3_600_000L;
    private static final long MINUTE_MS = 60_000L;

    private static long lastCheckMs;

    private ForceLoadIdleService() {
    }

    public static void onPlayerJoin(ServerPlayer player) {
        recordExtended(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        recordExtended(player);
    }

    private static void recordExtended(ServerPlayer player) {
        if (!ForceLoadIdleConfig.isEnabled()) {
            return;
        }
        ForceLoadIdleSavedData.get(player.server)
                .record(player.getUUID(), PermissionService.has(player, CointPermissionNodes.FORCELOAD_EXTENDED));
    }

    public static void tick(MinecraftServer server) {
        if (!ForceLoadIdleConfig.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCheckMs < ForceLoadIdleConfig.getCheckIntervalMinutes() * MINUTE_MS) {
            return;
        }
        lastCheckMs = now;
        if (!FtbIntegration.isAvailable()) {
            return;
        }
        try {
            releaseIdleTeams(server, now);
        } catch (RuntimeException exception) {
            LOGGER.error("Force-load idle check failed", exception);
        }
    }

    private static void releaseIdleTeams(MinecraftServer server, long now) {
        long idleMs = ForceLoadIdleConfig.getIdleHours() * HOUR_MS;
        ForceLoadIdleSavedData saved = ForceLoadIdleSavedData.get(server);
        Map<UUID, Collection<ClaimedChunk>> byTeam =
                FTBChunksAPI.api().getManager().getClaimedChunksByTeam(ClaimedChunk::isForceLoaded);
        CommandSourceStack source = server.createCommandSourceStack();

        for (Collection<ClaimedChunk> chunks : byTeam.values()) {
            if (chunks.isEmpty()) {
                continue;
            }
            ChunkTeamData data = chunks.iterator().next().getTeamData();
            Team team = data.getTeam();
            boolean extended = team.getMembers().stream().anyMatch(saved::isExtended);
            boolean unknown = team.getMembers().stream().anyMatch(id -> !saved.isKnown(id));
            if (!ForceLoadIdlePolicy.shouldUnforce(
                    team.isServerTeam(),
                    !team.getOnlineMembers().isEmpty(),
                    extended,
                    unknown,
                    data.getLastLoginTime(),
                    now,
                    idleMs
            )) {
                continue;
            }
            // unForceLoad mutates the team's chunk set; iterate over a copy of the positions.
            List<ChunkDimPos> positions = new ArrayList<>(chunks.size());
            for (ClaimedChunk chunk : chunks) {
                positions.add(chunk.getPos());
            }
            for (ChunkDimPos pos : positions) {
                data.unForceLoad(source, pos, false);
            }
            LOGGER.info(
                    "Force-load idle: released {} force-loaded chunk(s) of team {} ({}), idle {} h",
                    positions.size(),
                    team.getShortName(),
                    team.getTeamId(),
                    (now - data.getLastLoginTime()) / HOUR_MS
            );
        }
    }

    public static void resetRuntimeState() {
        lastCheckMs = 0L;
    }
}
