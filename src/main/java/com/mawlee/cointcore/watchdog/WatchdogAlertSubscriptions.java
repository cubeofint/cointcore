package com.mawlee.cointcore.watchdog;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Online preference snapshot, also safe for the watchdog thread when the server thread is stalled. */
public final class WatchdogAlertSubscriptions {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private WatchdogAlertSubscriptions() { }

    public static boolean isEnabled(UUID player) { return ENABLED.contains(player); }

    public static void set(UUID player, boolean enabled) {
        if (enabled) ENABLED.add(player);
        else ENABLED.remove(player);
    }

    public static void clear() { ENABLED.clear(); }
}
