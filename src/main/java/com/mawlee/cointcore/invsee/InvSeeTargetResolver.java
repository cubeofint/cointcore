package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.player.PlayerIdentityResolve;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Optional;
import java.util.UUID;

public final class InvSeeTargetResolver {
    private InvSeeTargetResolver() {
    }

    public static Optional<InvSeeTarget> resolve(MinecraftServer server, String name) {
        Optional<UUID> playerId = PlayerIdentityResolve.resolvePlayerId(server, name);
        if (playerId.isEmpty()) {
            return Optional.empty();
        }

        String resolvedName = PlayerIdentityResolve.resolveName(server, playerId.get()).orElse(name);
        ServerPlayer online = server.getPlayerList().getPlayer(playerId.get());
        if (online != null) {
            return Optional.of(InvSeeTarget.online(online));
        }

        return loadOffline(server, playerId.get(), resolvedName);
    }

    private static Optional<InvSeeTarget> loadOffline(MinecraftServer server, UUID playerId, String displayName) {
        GameProfile profile = server.getProfileCache()
                .get(playerId)
                .orElseGet(() -> new GameProfile(playerId, displayName));

        FakePlayer fakePlayer = new FakePlayer(server.overworld(), profile);
        Optional<CompoundTag> data = InvSeePlayerDataFiles.load(server, playerId);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        fakePlayer.load(data.get());
        return Optional.of(InvSeeTarget.offline(playerId, profile.getName(), fakePlayer));
    }
}
