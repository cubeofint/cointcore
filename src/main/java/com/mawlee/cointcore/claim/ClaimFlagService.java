package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.CointCoreFtbProperties;
import com.mawlee.cointcore.ftb.FtbIntegration;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ClaimFlagService {
    private ClaimFlagService() {
    }

    public static ClaimTeamFlags getFlags(Team team) {
        return ClaimTeamFlags.fromTeam(team);
    }

    public static void setDisablePlayerDamage(MinecraftServer server, Team team, boolean enabled) {
        setBooleanProperty(server, team, CointCoreFtbProperties.DISABLE_PLAYER_DAMAGE, enabled);
    }

    public static void setDisableHostileMobSpawn(MinecraftServer server, Team team, boolean enabled) {
        setBooleanProperty(server, team, CointCoreFtbProperties.DISABLE_HOSTILE_MOB_SPAWN, enabled);
    }

    public static void setProtectMobsFromOutsiders(MinecraftServer server, Team team, boolean enabled) {
        setBooleanProperty(server, team, CointCoreFtbProperties.PROTECT_MOBS_FROM_OUTSIDERS, enabled);
    }

    public static void migrateLegacyAllowPvpFlags(MinecraftServer server) {
        if (!FtbIntegration.isAvailable()) {
            return;
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        for (Team team : teamManager.getTeams()) {
            if (Boolean.TRUE.equals(team.getProperty(CointCoreFtbProperties.DISABLE_PLAYER_DAMAGE))) {
                continue;
            }

            if (Boolean.FALSE.equals(team.getProperty(FTBChunksProperties.ALLOW_PVP))) {
                setDisablePlayerDamage(server, team, true);
                team.setProperty(FTBChunksProperties.ALLOW_PVP, true);
                team.syncOnePropertyToAll(server, FTBChunksProperties.ALLOW_PVP, true);
            }
        }
    }

    public static boolean blocksPlayerDamage(ChunkTeamData teamData) {
        return Boolean.TRUE.equals(teamData.getTeam().getProperty(CointCoreFtbProperties.DISABLE_PLAYER_DAMAGE));
    }

    public static boolean blocksHostileMobSpawn(ChunkTeamData teamData) {
        return Boolean.TRUE.equals(teamData.getTeam().getProperty(CointCoreFtbProperties.DISABLE_HOSTILE_MOB_SPAWN));
    }

    public static boolean protectsMobsFromOutsiders(Team team) {
        if (!FtbIntegration.isAvailable()) {
            return false;
        }

        return Boolean.TRUE.equals(team.getProperty(CointCoreFtbProperties.PROTECT_MOBS_FROM_OUTSIDERS));
    }

    public static boolean blocksMobDamageFromPlayer(ServerPlayer player, LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        if (FtbIntegration.shouldPreventLivingEntityInteraction(player, mob)) {
            return true;
        }

        return FtbIntegration.getClaimTeamData(level, mob.blockPosition())
                .map(teamData -> protectsMobsFromOutsiders(teamData.getTeam()) && isOutsider(teamData, player))
                .orElse(false);
    }

    public static boolean isOutsider(ChunkTeamData teamData, ServerPlayer player) {
        return !teamData.isTeamMember(player.getUUID());
    }

    private static void setBooleanProperty(
            MinecraftServer server,
            Team team,
            dev.ftb.mods.ftbteams.api.property.BooleanProperty property,
            boolean enabled
    ) {
        if (!FtbIntegration.isAvailable()) {
            return;
        }

        team.setProperty(property, enabled);
        team.syncOnePropertyToAll(server, property, enabled);
    }
}
