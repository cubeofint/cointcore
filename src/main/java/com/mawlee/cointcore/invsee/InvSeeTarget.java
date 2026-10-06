package com.mawlee.cointcore.invsee;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public final class InvSeeTarget {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID playerId;
    private final InvSeeEditLock editLock = new InvSeeEditLock();
    private String displayName;
    private MinecraftServer server;
    private ServerPlayer onlinePlayer;
    private FakePlayer offlinePlayer;
    private CompoundTag originalNbt;
    private long fileLoadedAtMillis;
    private int refs;
    private long generation;
    private boolean dirty;
    private boolean handedOff;
    private boolean frozen;

    InvSeeTarget(UUID playerId, String displayName, MinecraftServer server) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.server = server;
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

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isUsable() {
        return getPlayer() != null;
    }

    public long generation() {
        return generation;
    }

    MinecraftServer server() {
        return server;
    }

    void acquire() {
        refs++;
    }

    int release() {
        refs = Math.max(0, refs - 1);
        if (refs == 0) {
            saveIfDirty();
        }
        return refs;
    }

    int refs() {
        return refs;
    }

    boolean attachOnline(ServerPlayer player) {
        this.server = player.server;
        this.onlinePlayer = player;
        this.offlinePlayer = null;
        this.originalNbt = null;
        this.displayName = player.getGameProfile().getName();
        this.dirty = false;
        this.handedOff = false;
        this.frozen = false;
        bumpGeneration();
        return true;
    }

    boolean attachOffline(MinecraftServer server, String displayName) {
        this.server = server;
        Optional<CompoundTag> data = InvSeePlayerDataFiles.load(server, playerId);
        if (data.isEmpty()) {
            return false;
        }
        GameProfile profile = server.getProfileCache()
                .get(playerId)
                .orElseGet(() -> new GameProfile(playerId, displayName));
        FakePlayer fakePlayer = new FakePlayer(server.overworld(), profile);
        fakePlayer.load(data.get());
        this.offlinePlayer = fakePlayer;
        this.onlinePlayer = null;
        this.originalNbt = data.get();
        this.fileLoadedAtMillis = InvSeePlayerDataFiles.lastModified(server, playerId);
        this.displayName = profile.getName();
        this.dirty = false;
        this.handedOff = false;
        this.frozen = false;
        bumpGeneration();
        return true;
    }

    public void markDirty() {
        if (isOffline() && !handedOff) {
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    void flushBeforeJoin() {
        frozen = true;
        editLock.forceRelease();
        if (offlinePlayer != null && dirty && !handedOff) {
            saveIfDirty();
        }
        handedOff = true;
        dirty = false;
    }

    void switchToOnline(ServerPlayer player) {
        if (onlinePlayer == player && !frozen && !handedOff) {
            return;
        }
        attachOnline(player);
        InvSeeAuditLog.targetOnline(playerId, displayName);
    }

    void freezeForLogout() {
        frozen = true;
        editLock.forceRelease();
        bumpGeneration();
    }

    void switchToOfflineAfterSave() {
        if (server == null) {
            return;
        }
        boolean loaded = attachOffline(server, displayName);
        if (!loaded) {
            LOGGER.warn("InvSee could not reload offline data for {} after logout", playerId);
            onlinePlayer = null;
            offlinePlayer = null;
            bumpGeneration();
        } else {
            InvSeeAuditLog.targetOffline(playerId, displayName);
        }
    }

    public void saveIfDirty() {
        if (!dirty || handedOff || offlinePlayer == null || server == null) {
            return;
        }

        long onDisk = InvSeePlayerDataFiles.lastModified(server, playerId);
        if (onDisk > fileLoadedAtMillis + 2_000L) {
            LOGGER.error(
                    "Refusing to overwrite newer playerdata for {} (disk={}, loaded={})",
                    playerId,
                    onDisk,
                    fileLoadedAtMillis
            );
            dirty = false;
            return;
        }

        CompoundTag latest = InvSeePlayerDataFiles.load(server, playerId).orElse(originalNbt);
        if (latest == null) {
            LOGGER.error("Cannot save InvSee offline data for {}: original NBT missing", playerId);
            return;
        }

        CompoundTag fakeSave = new CompoundTag();
        offlinePlayer.saveWithoutId(fakeSave);
        CompoundTag merged = InvSeeNbtMerge.overlayInventory(latest, fakeSave);
        if (InvSeePlayerDataFiles.save(server, playerId, merged)) {
            originalNbt = merged;
            fileLoadedAtMillis = InvSeePlayerDataFiles.lastModified(server, playerId);
            dirty = false;
        }
    }

    private void bumpGeneration() {
        generation++;
    }
}
