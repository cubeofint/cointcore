package com.mawlee.cointcore.kit;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks players who already received the starter kit on first join.
 * Reclaim cooldown is stored by FTB Essentials ({@code FTBEPlayerData}).
 */
public final class StarterKitSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_starter_kit";
    private static final String PLAYERS_KEY = "firstJoinGranted";

    private final Set<UUID> firstJoinGranted = new HashSet<>();

    private StarterKitSavedData() {
    }

    public static StarterKitSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(StarterKitSavedData::new, StarterKitSavedData::load), DATA_ID);
    }

    public boolean hasReceivedFirstJoin(UUID playerId) {
        return firstJoinGranted.contains(playerId);
    }

    public void markFirstJoinReceived(UUID playerId) {
        if (firstJoinGranted.add(playerId)) {
            setDirty();
        }
    }

    public boolean clearFirstJoin(UUID playerId) {
        if (firstJoinGranted.remove(playerId)) {
            setDirty();
            return true;
        }
        return false;
    }

    private static StarterKitSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        StarterKitSavedData data = new StarterKitSavedData();
        ListTag list = tag.getList(PLAYERS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                data.firstJoinGranted.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (UUID playerId : firstJoinGranted) {
            list.add(StringTag.valueOf(playerId.toString()));
        }
        tag.put(PLAYERS_KEY, list);
        return tag;
    }
}
