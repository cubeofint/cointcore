package com.mawlee.cointcore.invsee;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Optional;
import java.util.UUID;

public final class InvSeeTarget {
    private final UUID playerId;
    private final String displayName;
    private final ServerPlayer onlinePlayer;
    private final FakePlayer offlinePlayer;

    private InvSeeTarget(UUID playerId, String displayName, ServerPlayer onlinePlayer, FakePlayer offlinePlayer) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.onlinePlayer = onlinePlayer;
        this.offlinePlayer = offlinePlayer;
    }

    public static InvSeeTarget online(ServerPlayer player) {
        return new InvSeeTarget(player.getUUID(), player.getGameProfile().getName(), player, null);
    }

    public static InvSeeTarget offline(UUID playerId, String displayName, FakePlayer fakePlayer) {
        return new InvSeeTarget(playerId, displayName, null, fakePlayer);
    }

    public UUID playerId() {
        return playerId;
    }

    public String displayName() {
        return displayName;
    }

    public Player getPlayer() {
        return onlinePlayer != null ? onlinePlayer : offlinePlayer;
    }

    public Optional<ServerPlayer> onlinePlayer() {
        return Optional.ofNullable(onlinePlayer);
    }

    public boolean isOffline() {
        return onlinePlayer == null;
    }

    public void saveOffline(MinecraftServer server) {
        if (offlinePlayer != null) {
            CompoundTag data = new CompoundTag();
            offlinePlayer.saveWithoutId(data);
            InvSeePlayerDataFiles.save(server, playerId, data);
        }
    }
}
