package com.mawlee.cointcore.chatspy;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ChatSpySavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_chat_spy";
    private static final String ENABLED_KEY = "enabled";

    private final Set<UUID> enabled = new HashSet<>();

    private ChatSpySavedData() {
    }

    public static ChatSpySavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(ChatSpySavedData::new, ChatSpySavedData::load), DATA_ID);
    }

    public Set<UUID> enabledPlayers() {
        return Set.copyOf(enabled);
    }

    public boolean isEnabled(UUID playerId) {
        return enabled.contains(playerId);
    }

    public void setEnabled(UUID playerId, boolean spyEnabled) {
        if (spyEnabled) {
            if (enabled.add(playerId)) {
                setDirty();
            }
            return;
        }

        if (enabled.remove(playerId)) {
            setDirty();
        }
    }

    private static ChatSpySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ChatSpySavedData data = new ChatSpySavedData();
        if (!tag.contains(ENABLED_KEY, Tag.TAG_LIST)) {
            return data;
        }

        ListTag entries = tag.getList(ENABLED_KEY, Tag.TAG_STRING);
        for (int i = 0; i < entries.size(); i++) {
            try {
                data.enabled.add(UUID.fromString(entries.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag entries = new ListTag();
        for (UUID playerId : enabled) {
            entries.add(net.minecraft.nbt.StringTag.valueOf(playerId.toString()));
        }

        tag.put(ENABLED_KEY, entries);
        return tag;
    }
}
