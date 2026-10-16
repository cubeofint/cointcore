package com.mawlee.cointcore.slots;

/**
 * Minecraft-free login gate: public slots first, then reserved, then staff bypass.
 */
public final class ReservedSlotsRules {
    private ReservedSlotsRules() {
    }

    public enum Decision {
        ALLOW,
        PUBLIC_FULL,
        TOTAL_FULL
    }

    public static int totalSlots(int publicSlots, int reservedSlots) {
        int publicCap = Math.max(0, publicSlots);
        int reservedCap = Math.max(0, reservedSlots);
        if (publicCap > Integer.MAX_VALUE - reservedCap) {
            return Integer.MAX_VALUE;
        }
        return publicCap + reservedCap;
    }

    public static Decision decide(
            int onlineCount,
            int publicSlots,
            int reservedSlots,
            boolean reservedPermission,
            boolean bypassPermission
    ) {
        int online = Math.max(0, onlineCount);
        int publicCap = Math.max(0, publicSlots);
        int total = totalSlots(publicSlots, reservedSlots);
        if (online < publicCap) {
            return Decision.ALLOW;
        }
        if (bypassPermission) {
            return Decision.ALLOW;
        }
        if (online < total && reservedPermission) {
            return Decision.ALLOW;
        }
        if (online >= total) {
            return Decision.TOTAL_FULL;
        }
        return Decision.PUBLIC_FULL;
    }
}
