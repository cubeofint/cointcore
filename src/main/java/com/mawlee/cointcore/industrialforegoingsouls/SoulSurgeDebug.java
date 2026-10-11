package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.Locale;

/**
 * Text for {@code /cointcore soulsurge debug}. Kept Minecraft-free so the
 * wording can be unit-tested.
 */
public final class SoulSurgeDebug {
    private SoulSurgeDebug() {
    }

    public static String surgeLine(int x, int y, int z, String facing, int targetX, int targetY, int targetZ) {
        return String.format(
                Locale.ROOT,
                "Soul Surge at %d %d %d facing=%s target=%d %d %d",
                x, y, z, facing, targetX, targetY, targetZ
        );
    }

    public static String ownerLine(int targetX, int targetY, int targetZ, int surgeX, int surgeY, int surgeZ) {
        return String.format(
                Locale.ROOT,
                "Target %d %d %d claimed by surge at %d %d %d",
                targetX, targetY, targetZ, surgeX, surgeY, surgeZ
        );
    }

    public static String noOwnerLine(int x, int y, int z) {
        return String.format(Locale.ROOT, "No Soul Surge claim at %d %d %d", x, y, z);
    }
}
