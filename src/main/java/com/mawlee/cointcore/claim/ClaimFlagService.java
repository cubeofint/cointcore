package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.CointCoreFtbProperties;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.BooleanProperty;
import dev.ftb.mods.ftbteams.api.property.StringSetProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.Set;

public final class ClaimFlagService {
    public enum MobSpawnRule {
        ALLOW,
        DENY,
        CLEAR
    }

    private ClaimFlagService() {
    }

    public static boolean deniesAllMobSpawn(Team team) {
        return booleanValue(team, CointCoreFtbProperties.MOB_SPAWN_DENY_ALL, false);
    }

    public static boolean allowsMobDamage(Team team) {
        return booleanValue(team, CointCoreFtbProperties.MOB_DAMAGE, true);
    }

    public static boolean allowsFireSpread(Team team) {
        return booleanValue(team, CointCoreFtbProperties.FIRE_SPREAD, true);
    }

    public static boolean allowsPvp(Team team) {
        return booleanValue(team, FTBChunksProperties.ALLOW_PVP, true);
    }

    public static boolean isEntryMembersOnly(Team team) {
        return booleanValue(team, CointCoreFtbProperties.ENTRY_MEMBERS_ONLY, false);
    }

    public static Set<String> mobSpawnDenies(Team team) {
        return copySet(team.getProperty(CointCoreFtbProperties.MOB_SPAWN_DENY));
    }

    public static Set<String> mobSpawnAllows(Team team) {
        return copySet(team.getProperty(CointCoreFtbProperties.MOB_SPAWN_ALLOW));
    }

    public static boolean blocksMobSpawn(ChunkTeamData teamData, EntityType<?> type) {
        Team team = teamData.getTeam();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (mobSpawnAllows(team).contains(id)) {
            return false;
        }
        if (mobSpawnDenies(team).contains(id)) {
            return true;
        }
        return deniesAllMobSpawn(team);
    }

    public static boolean blocksMobDamageToPlayer(ChunkTeamData teamData) {
        return !allowsMobDamage(teamData.getTeam());
    }

    public static boolean blocksFireSpread(ServerLevel level, BlockPos pos) {
        return FtbIntegration.getClaimTeamData(level, pos)
                .map(teamData -> !allowsFireSpread(teamData.getTeam()))
                .orElse(false);
    }

    public static boolean blocksPvp(ChunkTeamData teamData) {
        return !allowsPvp(teamData.getTeam());
    }

    public static boolean deniesEntry(ChunkTeamData teamData, ServerPlayer player) {
        if (!isEntryMembersOnly(teamData.getTeam())) {
            return false;
        }
        if (player.hasPermissions(2) || PermissionService.has(player, CointPermissionNodes.CLAIM_FLAG_ENTRY_BYPASS)) {
            return false;
        }
        return !teamData.isTeamMember(player.getUUID());
    }

    /**
     * Outsiders already fail {@code INTERACT_ENTITY}. That check is what stops them from hitting mobs.
     * Machine farms deal the hit as a {@link FakePlayer} and must keep working.
     */
    public static boolean blocksMobDamageFromPlayer(ServerPlayer player, LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel)) {
            return false;
        }
        if (player instanceof FakePlayer) {
            return false;
        }
        return FtbIntegration.shouldPreventLivingEntityInteraction(player, mob);
    }

    public static void setMobSpawnDeniedAll(MinecraftServer server, Team team, boolean denied) {
        setBoolean(server, team, CointCoreFtbProperties.MOB_SPAWN_DENY_ALL, denied);
    }

    public static void setMobSpawnRule(MinecraftServer server, Team team, ResourceLocation mobId, MobSpawnRule rule) {
        Set<String> deny = mobSpawnDenies(team);
        Set<String> allow = mobSpawnAllows(team);
        String id = mobId.toString();
        deny.remove(id);
        allow.remove(id);
        switch (rule) {
            case ALLOW -> allow.add(id);
            case DENY -> deny.add(id);
            case CLEAR -> {
            }
        }
        setStringSet(server, team, CointCoreFtbProperties.MOB_SPAWN_ALLOW, allow);
        setStringSet(server, team, CointCoreFtbProperties.MOB_SPAWN_DENY, deny);
    }

    public static void setMobDamage(MinecraftServer server, Team team, boolean allowed) {
        setBoolean(server, team, CointCoreFtbProperties.MOB_DAMAGE, allowed);
    }

    public static void setFireSpread(MinecraftServer server, Team team, boolean allowed) {
        setBoolean(server, team, CointCoreFtbProperties.FIRE_SPREAD, allowed);
    }

    public static void setPvp(MinecraftServer server, Team team, boolean allowed) {
        setBoolean(server, team, FTBChunksProperties.ALLOW_PVP, allowed);
    }

    public static void setEntryMembersOnly(MinecraftServer server, Team team, boolean membersOnly) {
        setBoolean(server, team, CointCoreFtbProperties.ENTRY_MEMBERS_ONLY, membersOnly);
    }

    public static void migrateRemovedFlags(MinecraftServer server) {
        if (!FtbIntegration.isAvailable() || CointCoreFtbProperties.LEGACY_DISABLE_PLAYER_DAMAGE == null) {
            return;
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        for (Team team : teamManager.getTeams()) {
            if (!Boolean.TRUE.equals(team.getProperty(CointCoreFtbProperties.LEGACY_DISABLE_PLAYER_DAMAGE))) {
                continue;
            }
            setPvp(server, team, false);
            team.setProperty(CointCoreFtbProperties.LEGACY_DISABLE_PLAYER_DAMAGE, false);
            team.markDirty();
        }
    }

    private static boolean booleanValue(Team team, BooleanProperty property, boolean defaultValue) {
        if (team == null || property == null) {
            return defaultValue;
        }
        Boolean value = team.getProperty(property);
        return value == null ? defaultValue : value;
    }

    private static Set<String> copySet(Set<String> value) {
        if (value == null || value.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(value);
    }

    private static void setBoolean(MinecraftServer server, Team team, BooleanProperty property, boolean value) {
        if (!FtbIntegration.isAvailable() || property == null) {
            return;
        }
        team.setProperty(property, value);
        team.syncOnePropertyToAll(server, property, value);
    }

    private static void setStringSet(MinecraftServer server, Team team, StringSetProperty property, Set<String> value) {
        if (!FtbIntegration.isAvailable() || property == null) {
            return;
        }
        Set<String> stored = Set.copyOf(value);
        team.setProperty(property, stored);
        team.syncOnePropertyToAll(server, property, stored);
    }
}
