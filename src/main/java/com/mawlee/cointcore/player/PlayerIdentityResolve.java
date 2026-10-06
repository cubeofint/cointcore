package com.mawlee.cointcore.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.UsernameCache;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves player UUID/name for moderation commands.
 * Order: online → usercache ({@code GameProfileCache}) → NeoForge {@code usernamecache.json}.
 */
public final class PlayerIdentityResolve {
    private PlayerIdentityResolve() {
    }

    public static Optional<UUID> resolvePlayerId(MinecraftServer server, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String trimmed = name.trim();
        Optional<UUID> asUuid = tryParseUuid(trimmed);
        if (asUuid.isPresent()) {
            return asUuid;
        }

        ServerPlayer onlineMatch = null;
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (!online.getGameProfile().getName().equalsIgnoreCase(trimmed)) {
                continue;
            }
            if (onlineMatch != null) {
                return Optional.empty();
            }
            onlineMatch = online;
        }
        if (onlineMatch != null) {
            return Optional.of(onlineMatch.getUUID());
        }

        Optional<UUID> fromUsercache = server.getProfileCache()
                .get(trimmed)
                .map(GameProfile::getId);
        if (fromUsercache.isPresent()) {
            return fromUsercache;
        }

        return resolveFromUsernameCache(server, trimmed);
    }

    public static Optional<String> resolveName(MinecraftServer server, UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }

        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }

        Optional<String> fromUsercache = server.getProfileCache()
                .get(playerId)
                .map(GameProfile::getName);
        if (fromUsercache.isPresent()) {
            return fromUsercache;
        }

        String known = UsernameCache.getLastKnownUsername(playerId);
        if (known != null && !known.isBlank()) {
            return Optional.of(known);
        }

        return Optional.empty();
    }

    private static Optional<UUID> resolveFromUsernameCache(MinecraftServer server, String name) {
        Map<UUID, String> map = UsernameCache.getMap();
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        UUID exact = null;
        UUID ignoreCase = null;
        int ignoreCaseCount = 0;

        for (Map.Entry<UUID, String> entry : map.entrySet()) {
            String cached = entry.getValue();
            if (cached == null) {
                continue;
            }
            if (cached.equals(name)) {
                exact = entry.getKey();
                break;
            }
            if (cached.equalsIgnoreCase(name)) {
                ignoreCase = entry.getKey();
                ignoreCaseCount++;
            }
        }

        UUID resolved = exact != null ? exact : (ignoreCaseCount == 1 ? ignoreCase : null);
        if (resolved == null) {
            return Optional.empty();
        }

        String canonical = UsernameCache.getLastKnownUsername(resolved);
        if (canonical == null || canonical.isBlank()) {
            canonical = name;
        }
        try {
            server.getProfileCache().add(new GameProfile(resolved, canonical));
        } catch (RuntimeException ignored) {
            // Cache write is best-effort; ban still works with UUID+name.
        }
        return Optional.of(resolved);
    }

    private static Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
