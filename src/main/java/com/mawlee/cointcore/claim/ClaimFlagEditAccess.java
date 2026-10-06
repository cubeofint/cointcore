package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.CointCoreFtbProperties;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public final class ClaimFlagEditAccess {
    public static final int MOB_SPAWN = 1;
    public static final int MOB_DAMAGE = 1 << 1;
    public static final int FIRE_SPREAD = 1 << 2;
    public static final int ENTRY = 1 << 3;

    private ClaimFlagEditAccess() {
    }

    public static int maskFor(ServerPlayer player) {
        int mask = 0;
        if (canEdit(player, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN)) {
            mask |= MOB_SPAWN;
        }
        if (canEdit(player, CointPermissionNodes.CLAIM_FLAG_MOB_DAMAGE)) {
            mask |= MOB_DAMAGE;
        }
        if (canEdit(player, CointPermissionNodes.CLAIM_FLAG_FIRE_SPREAD)) {
            mask |= FIRE_SPREAD;
        }
        if (canEdit(player, CointPermissionNodes.CLAIM_FLAG_ENTRY)) {
            mask |= ENTRY;
        }
        return mask;
    }

    public static boolean canEdit(ServerPlayer player, PermissionNode<Boolean> node) {
        return player.hasPermissions(2) || PermissionService.has(player, node);
    }

    public static Integer maskBit(String namespace, String path) {
        if (!"ftbchunks".equals(namespace)) {
            return null;
        }
        return switch (path) {
            case "mob_spawn_deny_all", "mob_spawn_deny", "mob_spawn_allow" -> MOB_SPAWN;
            case "mob_damage" -> MOB_DAMAGE;
            case "fire_spread" -> FIRE_SPREAD;
            case "entry_members_only" -> ENTRY;
            default -> null;
        };
    }

    public static void revertUnauthorized(ServerPlayer player, Team team, TeamPropertyCollection incoming) {
        revert(player, team, incoming, CointCoreFtbProperties.MOB_SPAWN_DENY_ALL, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN);
        revert(player, team, incoming, CointCoreFtbProperties.MOB_SPAWN_DENY, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN);
        revert(player, team, incoming, CointCoreFtbProperties.MOB_SPAWN_ALLOW, CointPermissionNodes.CLAIM_FLAG_MOB_SPAWN);
        revert(player, team, incoming, CointCoreFtbProperties.MOB_DAMAGE, CointPermissionNodes.CLAIM_FLAG_MOB_DAMAGE);
        revert(player, team, incoming, CointCoreFtbProperties.FIRE_SPREAD, CointPermissionNodes.CLAIM_FLAG_FIRE_SPREAD);
        revert(player, team, incoming, CointCoreFtbProperties.ENTRY_MEMBERS_ONLY, CointPermissionNodes.CLAIM_FLAG_ENTRY);
    }

    private static <T> void revert(
            ServerPlayer player,
            Team team,
            TeamPropertyCollection incoming,
            TeamProperty<T> property,
            PermissionNode<Boolean> node
    ) {
        if (property == null || canEdit(player, node)) {
            return;
        }
        incoming.set(property, team.getProperty(property));
    }
}
