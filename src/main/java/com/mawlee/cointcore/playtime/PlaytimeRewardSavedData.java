package com.mawlee.cointcore.playtime;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlaytimeRewardSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_playtime_reward";
    private static final String PLAYERS_KEY = "players";

    private final Map<UUID, Long> accumulatedMs = new HashMap<>();

    private PlaytimeRewardSavedData() {
    }

    public static PlaytimeRewardSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(PlaytimeRewardSavedData::new, PlaytimeRewardSavedData::load),
                        DATA_ID
                );
    }

    public synchronized long getAccumulatedMs(UUID playerId) {
        return Math.max(0L, accumulatedMs.getOrDefault(playerId, 0L));
    }

    public synchronized void setAccumulatedMs(UUID playerId, long millis) {
        long normalized = Math.max(0L, millis);
        if (normalized == 0L) {
            if (accumulatedMs.remove(playerId) != null) {
                setDirty();
            }
            return;
        }
        Long previous = accumulatedMs.put(playerId, normalized);
        if (previous == null || previous != normalized) {
            setDirty();
        }
    }

    private static PlaytimeRewardSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlaytimeRewardSavedData data = new PlaytimeRewardSavedData();
        CompoundTag players = tag.getCompound(PLAYERS_KEY);
        for (String rawId : players.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(rawId);
                long value = players.getLong(rawId);
                if (value > 0L) {
                    data.accumulatedMs.put(playerId, value);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : accumulatedMs.entrySet()) {
            if (entry.getValue() > 0L) {
                players.putLong(entry.getKey().toString(), entry.getValue());
            }
        }
        tag.put(PLAYERS_KEY, players);
        return tag;
    }
}
