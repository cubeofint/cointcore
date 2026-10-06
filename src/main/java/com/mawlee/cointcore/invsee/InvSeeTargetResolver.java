package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.player.PlayerIdentityResolve;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class InvSeeTargetResolver {
    private InvSeeTargetResolver() {
    }

    public static Optional<InvSeeTarget> acquire(MinecraftServer server, String name) {
        Optional<UUID> playerId = PlayerIdentityResolve.resolvePlayerId(server, name);
        if (playerId.isEmpty()) {
            return Optional.empty();
        }
        String resolvedName = PlayerIdentityResolve.resolveName(server, playerId.get()).orElse(name);
        return InvSeeTargets.acquire(server, playerId.get(), resolvedName);
    }

    public static Optional<UUID> resolvePlayerId(MinecraftServer server, String name) {
        return PlayerIdentityResolve.resolvePlayerId(server, name);
    }

    /**
     * Suggestion helper: never reads playerdata. Uses an online player or an already-open target.
     */
    public static Optional<Player> suggestionPlayer(MinecraftServer server, String name) {
        Optional<UUID> playerId = PlayerIdentityResolve.resolvePlayerId(server, name);
        if (playerId.isEmpty()) {
            return Optional.empty();
        }
        ServerPlayer online = server.getPlayerList().getPlayer(playerId.get());
        if (online != null) {
            return Optional.of(online);
        }
        return InvSeeTargets.getIfPresent(playerId.get()).map(InvSeeTarget::getPlayer);
    }
}
