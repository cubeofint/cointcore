package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.ftb.CointCoreFtbProperties;
import dev.ftb.mods.ftbteams.api.Team;

public record ClaimTeamFlags(
        boolean disablePlayerDamage,
        boolean disableHostileMobSpawn,
        boolean protectMobsFromOutsiders
) {
    public static ClaimTeamFlags fromTeam(Team team) {
        return new ClaimTeamFlags(
                team.getProperty(CointCoreFtbProperties.DISABLE_PLAYER_DAMAGE),
                team.getProperty(CointCoreFtbProperties.DISABLE_HOSTILE_MOB_SPAWN),
                team.getProperty(CointCoreFtbProperties.PROTECT_MOBS_FROM_OUTSIDERS)
        );
    }
}
