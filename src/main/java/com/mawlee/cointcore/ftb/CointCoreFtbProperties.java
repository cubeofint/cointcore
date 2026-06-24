package com.mawlee.cointcore.ftb;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.property.BooleanProperty;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;

public final class CointCoreFtbProperties {
    public static BooleanProperty DISABLE_HOSTILE_MOB_SPAWN;
    public static BooleanProperty DISABLE_PLAYER_DAMAGE;
    public static BooleanProperty PROTECT_MOBS_FROM_OUTSIDERS;

    private CointCoreFtbProperties() {
    }

    public static void init() {
        if (DISABLE_HOSTILE_MOB_SPAWN != null) {
            return;
        }

        DISABLE_HOSTILE_MOB_SPAWN = booleanProperty("disable_hostile_mob_spawn");
        DISABLE_PLAYER_DAMAGE = booleanProperty("disable_player_damage");
        PROTECT_MOBS_FROM_OUTSIDERS = booleanProperty("protect_mobs_from_outsiders");
    }

    private static BooleanProperty booleanProperty(String name) {
        TeamProperty<Boolean> property = new BooleanProperty(FTBChunksAPI.rl(name), false).syncToAll();
        return (BooleanProperty) property;
    }
}
