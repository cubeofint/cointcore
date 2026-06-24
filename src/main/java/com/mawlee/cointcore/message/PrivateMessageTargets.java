package com.mawlee.cointcore.message;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivateMessageTargets {
    private static final Map<UUID, UUID> LAST_PARTNER = new ConcurrentHashMap<>();

    private PrivateMessageTargets() {
    }

    public static void link(UUID first, UUID second) {
        LAST_PARTNER.put(first, second);
        LAST_PARTNER.put(second, first);
    }

    public static UUID getLastPartner(UUID playerId) {
        return LAST_PARTNER.get(playerId);
    }

    public static void forget(UUID playerId) {
        LAST_PARTNER.remove(playerId);
    }

    public static void clearRuntimeState() {
        LAST_PARTNER.clear();
    }
}
