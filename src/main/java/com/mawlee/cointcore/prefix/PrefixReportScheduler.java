package com.mawlee.cointcore.prefix;

import com.mawlee.cointcore.config.PrefixConfig;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debounces game → site prefix reports (join, LP recalc, {@code /prefix} change).
 */
public final class PrefixReportScheduler {
    private static final Map<UUID, Integer> DUE = new ConcurrentHashMap<>();
    private static int tick;

    private PrefixReportScheduler() {
    }

    public static void request(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null || !PrefixConfig.get().siteSyncEnabled()) {
            return;
        }
        DUE.put(playerId, tick + PrefixConfig.get().siteReportDebounceTicks());
    }

    public static void tick(MinecraftServer server) {
        tick++;
        if (!PrefixConfig.get().enabled() || !PrefixConfig.get().siteSyncEnabled()) {
            return;
        }
        List<UUID> ready = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : DUE.entrySet()) {
            if (entry.getValue() <= tick) {
                ready.add(entry.getKey());
            }
        }
        if (ready.isEmpty()) {
            return;
        }
        for (UUID playerId : ready) {
            DUE.remove(playerId);
        }
        PrefixSiteBridge.report(server, ready);
    }
}
