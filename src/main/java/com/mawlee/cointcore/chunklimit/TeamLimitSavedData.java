package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists per-chunk team block-limit contributions so unloaded claims keep counting across restarts. */
public final class TeamLimitSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_team_limits";

    public record Entry(String dimension, long chunk, UUID teamId, Map<String, Integer> counts) {
    }

    private final java.util.List<Entry> loaded = new java.util.ArrayList<>();

    private TeamLimitSavedData() {
    }

    public static TeamLimitSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TeamLimitSavedData::new, TeamLimitSavedData::load), DATA_ID);
    }

    /** Entries read from disk; consumed once by {@link TeamLimitIndex}. */
    java.util.List<Entry> drainLoaded() {
        java.util.List<Entry> copy = new java.util.ArrayList<>(loaded);
        loaded.clear();
        return copy;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        TeamLimitIndex.forEachContribution((dimension, chunk, teamId, counts) -> {
            if (counts.isEmpty()) {
                return;
            }
            CompoundTag item = new CompoundTag();
            item.putString("d", dimension);
            item.putLong("c", chunk);
            item.putUUID("t", teamId);
            CompoundTag k = new CompoundTag();
            counts.forEach(k::putInt);
            item.put("k", k);
            list.add(item);
        });
        tag.put("entries", list);
        return tag;
    }

    private static TeamLimitSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TeamLimitSavedData data = new TeamLimitSavedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (!item.hasUUID("t")) {
                continue;
            }
            CompoundTag k = item.getCompound("k");
            Map<String, Integer> counts = new HashMap<>();
            for (String key : k.getAllKeys()) {
                int v = k.getInt(key);
                if (v > 0) {
                    counts.put(key, v);
                }
            }
            data.loaded.add(new Entry(item.getString("d"), item.getLong("c"), item.getUUID("t"), counts));
        }
        return data;
    }
}
