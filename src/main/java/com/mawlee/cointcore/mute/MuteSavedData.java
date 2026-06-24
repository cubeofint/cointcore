package com.mawlee.cointcore.mute;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MuteSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_mutes";
    private static final String ENTRIES_KEY = "entries";

    private final Map<UUID, MuteInfo> mutes = new HashMap<>();

    private MuteSavedData() {
    }

    public static MuteSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(MuteSavedData::new, MuteSavedData::load), DATA_ID);
    }

    public Optional<MuteInfo> get(UUID playerId) {
        MuteInfo info = mutes.get(playerId);
        if (info == null || info.isExpired()) {
            if (info != null) {
                mutes.remove(playerId);
                setDirty();
            }
            return Optional.empty();
        }

        return Optional.of(info);
    }

    public void put(UUID playerId, MuteInfo info) {
        mutes.put(playerId, info);
        setDirty();
    }

    public void remove(UUID playerId) {
        if (mutes.remove(playerId) != null) {
            setDirty();
        }
    }

    public boolean cleanupIfExpired(UUID playerId) {
        MuteInfo info = mutes.get(playerId);
        if (info == null || !info.isExpired()) {
            return false;
        }

        mutes.remove(playerId);
        setDirty();
        return true;
    }

    private static MuteSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MuteSavedData data = new MuteSavedData();
        if (!tag.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
            return data;
        }

        ListTag entries = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            try {
                UUID playerId = UUID.fromString(entry.getString("player"));
                MuteInfo info = new MuteInfo(
                        entry.getString("muter"),
                        entry.getString("reason"),
                        entry.getLong("expiresAt")
                );
                if (!info.isExpired()) {
                    data.mutes.put(playerId, info);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag entries = new ListTag();
        mutes.entrySet().removeIf(entry -> entry.getValue().isExpired());

        for (Map.Entry<UUID, MuteInfo> entry : mutes.entrySet()) {
            CompoundTag stored = new CompoundTag();
            stored.putString("player", entry.getKey().toString());
            stored.putString("muter", entry.getValue().muter());
            stored.putString("reason", entry.getValue().reason());
            stored.putLong("expiresAt", entry.getValue().expiresAt());
            entries.add(stored);
        }

        tag.put(ENTRIES_KEY, entries);
        return tag;
    }
}
