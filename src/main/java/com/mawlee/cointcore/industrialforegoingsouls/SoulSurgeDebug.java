package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.Locale;

/**
 * Text for {@code /cointcore soulsurge debug}. Kept Minecraft-free so the
 * wording can be unit-tested.
 */
public final class SoulSurgeDebug {
    private SoulSurgeDebug() {
    }

    public static String surgeLine(
            int x,
            int y,
            int z,
            String facing,
            int targetX,
            int targetY,
            int targetZ,
            int extraTicks,
            int maxExtraTicks
    ) {
        return String.format(
                Locale.ROOT,
                "Soul Surge at %d %d %d facing=%s target=%d %d %d extra=%d/%d",
                x, y, z, facing, targetX, targetY, targetZ, extraTicks, maxExtraTicks
        );
    }

    public static String targetLine(int x, int y, int z, int extraTicks, int maxExtraTicks) {
        return String.format(
                Locale.ROOT,
                "Target %d %d %d extra=%d/%d",
                x, y, z, extraTicks, maxExtraTicks
        );
    }
}
