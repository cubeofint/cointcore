package com.mawlee.cointcore.ftb;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.property.BooleanProperty;
import dev.ftb.mods.ftbteams.api.property.StringSetProperty;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;

import java.util.Set;

public final class CointCoreFtbProperties {
    /** When true, mobs do not spawn in the team's claims unless a specific mob is allowed. */
    public static BooleanProperty MOB_SPAWN_DENY_ALL;
    /** Entity ids that cannot spawn even when the general rule allows spawning. */
    public static StringSetProperty MOB_SPAWN_DENY;
    /** Entity ids that can spawn even when the general rule denies spawning. */
    public static StringSetProperty MOB_SPAWN_ALLOW;
    /** When false, mobs and their projectiles do not damage players inside the claim. */
    public static BooleanProperty MOB_DAMAGE;
    /** When false, lava, flint and steel, and fire charges do not start or spread fire. */
    public static BooleanProperty FIRE_SPREAD;
    /** When true, only team members may enter the team's claims. Allies stay outside. */
    public static BooleanProperty ENTRY_MEMBERS_ONLY;
    /**
     * Previous {@code disable_player_damage} value. Kept hidden so an existing team file can be
     * copied onto {@code ALLOW_PVP} once, then cleared.
     */
    public static BooleanProperty LEGACY_DISABLE_PLAYER_DAMAGE;

    private CointCoreFtbProperties() {
    }

    public static void init() {
        if (MOB_SPAWN_DENY_ALL != null) {
            return;
        }

        MOB_SPAWN_DENY_ALL = booleanProperty("mob_spawn_deny_all", false);
        MOB_SPAWN_DENY = stringSetProperty("mob_spawn_deny");
        MOB_SPAWN_ALLOW = stringSetProperty("mob_spawn_allow");
        MOB_DAMAGE = booleanProperty("mob_damage", true);
        FIRE_SPREAD = booleanProperty("fire_spread", true);
        ENTRY_MEMBERS_ONLY = booleanProperty("entry_members_only", false);
        LEGACY_DISABLE_PLAYER_DAMAGE = (BooleanProperty) hide(new BooleanProperty(
                FTBChunksAPI.rl("disable_player_damage"),
                false
        ).notPlayerEditable());
    }

    private static BooleanProperty booleanProperty(String name, boolean defaultValue) {
        return (BooleanProperty) new BooleanProperty(FTBChunksAPI.rl(name), defaultValue).syncToAll();
    }

    private static StringSetProperty stringSetProperty(String name) {
        return (StringSetProperty) new StringSetProperty(FTBChunksAPI.rl(name), Set.of()).syncToAll();
    }

    /**
     * {@code hidden()} exists on FTB Teams 2101.1.11, which is newer than the compile-time API.
     * Only the retired {@code disable_player_damage} value stays hidden.
     */
    private static TeamProperty<?> hide(TeamProperty<?> property) {
        try {
            return (TeamProperty<?>) TeamProperty.class.getMethod("hidden").invoke(property);
        } catch (ReflectiveOperationException exception) {
            return property;
        }
    }
}
