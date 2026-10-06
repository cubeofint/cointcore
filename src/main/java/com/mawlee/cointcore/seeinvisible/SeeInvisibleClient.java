package com.mawlee.cointcore.seeinvisible;

import net.minecraft.world.entity.player.Player;

/**
 * Client-side flag synced from the server for {@code cointcore.vanish.see}.
 */
public final class SeeInvisibleClient {
    private static boolean canSeeInvisible;

    private SeeInvisibleClient() {
    }

    public static void setCanSeeInvisible(boolean canSee) {
        canSeeInvisible = canSee;
    }

    public static void clear() {
        canSeeInvisible = false;
    }

    public static boolean canSeeInvisible() {
        return canSeeInvisible;
    }

    public static boolean canSeeInvisible(Player viewer) {
        return canSeeInvisible && viewer != null;
    }
}
