package com.mawlee.cointcore.kit;

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

public final class KitCreditSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_kit_credits";
    private static final String PLAYERS_KEY = "players";

    private final KitCreditStore store = new KitCreditStore();

    private KitCreditSavedData() {
    }

    public static KitCreditSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(KitCreditSavedData::new, KitCreditSavedData::load), DATA_ID);
    }

    public int getCredits(UUID playerId, String kitName) {
        return store.get(playerId, kitName);
    }

    public int setCredits(UUID playerId, String kitName, int amount) {
        int total = store.set(playerId, kitName, amount);
        setDirty();
        return total;
    }

    public int addCredits(UUID playerId, String kitName, int amount) {
        int total = store.add(playerId, kitName, amount);
        setDirty();
        return total;
    }

    public int takeCredits(UUID playerId, String kitName, int amount) {
        int total = store.take(playerId, kitName, amount);
        setDirty();
        return total;
    }

    public Map<String, Integer> getAllCredits(UUID playerId) {
        return store.all(playerId);
    }

    public Map<String, Integer> clearAll(UUID playerId) {
        Map<String, Integer> previous = store.clearAll(playerId);
        if (!previous.isEmpty()) {
            setDirty();
        }
        return previous;
    }

    public int clearKit(UUID playerId, String kitName) {
        int previous = store.clearKit(playerId, kitName);
        if (previous > 0) {
            setDirty();
        }
        return previous;
    }

    private static KitCreditSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        KitCreditSavedData data = new KitCreditSavedData();
        if (!tag.contains(PLAYERS_KEY, Tag.TAG_LIST)) {
            return data;
        }

        Map<UUID, Map<String, Integer>> loaded = new HashMap<>();
        ListTag players = tag.getList(PLAYERS_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : players) {
            CompoundTag entry = (CompoundTag) entryTag;
            try {
                // save() writes IntArray via putUUID; getString() returns "" and skips the entry.
                UUID playerId = entry.hasUUID("player_id")
                        ? entry.getUUID("player_id")
                        : UUID.fromString(entry.getString("player_id"));
                CompoundTag kitsTag = entry.getCompound("kits");
                Map<String, Integer> kits = new HashMap<>();

                for (String kitName : kitsTag.getAllKeys()) {
                    int amount = kitsTag.getInt(kitName);
                    if (amount > 0) {
                        kits.put(kitName, amount);
                    }
                }

                if (!kits.isEmpty()) {
                    loaded.put(playerId, kits);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        data.store.replaceAll(loaded);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();

        for (Map.Entry<UUID, Map<String, Integer>> entry : store.snapshot().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player_id", entry.getKey());

            CompoundTag kitsTag = new CompoundTag();
            for (Map.Entry<String, Integer> kitEntry : entry.getValue().entrySet()) {
                if (kitEntry.getValue() > 0) {
                    kitsTag.putInt(kitEntry.getKey(), kitEntry.getValue());
                }
            }

            if (!kitsTag.isEmpty()) {
                playerTag.put("kits", kitsTag);
                players.add(playerTag);
            }
        }

        tag.put(PLAYERS_KEY, players);
        return tag;
    }
}
