package com.mawlee.cointcore.invsee;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.UsernameCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory name suggestions. Never opens playerdata files.
 */
public final class InvSeeNameSuggestions {
    private static final long CACHE_MS = 5_000L;

    private static List<String> cached = List.of();
    private static long cachedAt;

    private InvSeeNameSuggestions() {
    }

    public static List<String> suggest(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_MS && !cached.isEmpty()) {
            return mergeOnline(server, cached);
        }

        Set<String> names = new LinkedHashSet<>();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }

        Map<UUID, String> usernameCache = UsernameCache.getMap();
        if (usernameCache != null) {
            for (String name : usernameCache.values()) {
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }

        cached = List.copyOf(names);
        cachedAt = now;
        return cached;
    }

    private static List<String> mergeOnline(MinecraftServer server, List<String> base) {
        Collection<ServerPlayer> online = server.getPlayerList().getPlayers();
        if (online.isEmpty()) {
            return base;
        }
        LinkedHashSet<String> names = new LinkedHashSet<>(base);
        for (ServerPlayer player : online) {
            names.add(player.getGameProfile().getName());
        }
        return new ArrayList<>(names);
    }
}
