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

    private final Map<UUID, Map<String, Integer>> creditsByPlayer = new HashMap<>();

    private KitCreditSavedData() {
    }

    public static KitCreditSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(KitCreditSavedData::new, KitCreditSavedData::load), DATA_ID);
    }

    public int getCredits(UUID playerId, String kitName) {
        Map<String, Integer> kits = creditsByPlayer.get(playerId);
        if (kits == null) {
            return 0;
        }

        return Math.max(0, kits.getOrDefault(normalizeKitName(kitName), 0));
    }

    public int setCredits(UUID playerId, String kitName, int amount) {
        int normalized = Math.max(0, amount);
        Map<String, Integer> kits = creditsByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>());
        String key = normalizeKitName(kitName);

        if (normalized == 0) {
            kits.remove(key);
            cleanupPlayer(playerId, kits);
        } else {
            kits.put(key, normalized);
        }

        setDirty();
        return normalized;
    }

    public int addCredits(UUID playerId, String kitName, int amount) {
        if (amount <= 0) {
            return getCredits(playerId, kitName);
        }

        return setCredits(playerId, kitName, getCredits(playerId, kitName) + amount);
    }

    public int takeCredits(UUID playerId, String kitName, int amount) {
        if (amount <= 0) {
            return getCredits(playerId, kitName);
        }

        return setCredits(playerId, kitName, Math.max(0, getCredits(playerId, kitName) - amount));
    }

    public Map<String, Integer> getAllCredits(UUID playerId) {
        Map<String, Integer> kits = creditsByPlayer.get(playerId);
        if (kits == null || kits.isEmpty()) {
            return Map.of();
        }

        return Map.copyOf(kits);
    }

    private void cleanupPlayer(UUID playerId, Map<String, Integer> kits) {
        if (kits.isEmpty()) {
            creditsByPlayer.remove(playerId);
        }
    }

    private static String normalizeKitName(String kitName) {
        return kitName.toLowerCase();
    }

    private static KitCreditSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        KitCreditSavedData data = new KitCreditSavedData();
        if (!tag.contains(PLAYERS_KEY, Tag.TAG_LIST)) {
            return data;
        }

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
                        kits.put(normalizeKitName(kitName), amount);
                    }
                }

                if (!kits.isEmpty()) {
                    data.creditsByPlayer.put(playerId, kits);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();

        for (Map.Entry<UUID, Map<String, Integer>> entry : creditsByPlayer.entrySet()) {
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
