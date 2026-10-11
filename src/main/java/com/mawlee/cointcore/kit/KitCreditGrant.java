package com.mawlee.cointcore.kit;

import java.util.UUID;

/**
 * Pure decision rules for the AzLink reflection entry
 * {@code KitCreditService.addCredits(UUID, String, int)}.
 */
public final class KitCreditGrant {
    public static final String APPLIED = "applied";
    public static final String SCHEDULED = "scheduled";
    public static final String NO_SERVER = "no_server";
    public static final String INVALID = "invalid";

    private KitCreditGrant() {
    }

    public record Decision(String status, boolean applyNow, boolean schedule) {
        public static Decision of(String status) {
            return new Decision(status, APPLIED.equals(status), SCHEDULED.equals(status));
        }
    }

    public static boolean validGrant(UUID uuid, String kit, int qty) {
        return uuid != null && KitCreditStore.normalizeKitName(kit) != null && qty > 0;
    }

    public static Decision decide(UUID uuid, String kit, int qty, boolean hasServer, boolean sameThread) {
        if (!validGrant(uuid, kit, qty)) {
            return Decision.of(INVALID);
        }
        if (!hasServer) {
            return Decision.of(NO_SERVER);
        }
        if (!sameThread) {
            return Decision.of(SCHEDULED);
        }
        return Decision.of(APPLIED);
    }
}
