package com.mawlee.cointcore.shop;

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

public final class GluonWalletSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_gluon_wallets";
    private static final String PLAYERS_KEY = "players";

    private final Map<UUID, Long> balances = new HashMap<>();

    private GluonWalletSavedData() {
    }

    public static GluonWalletSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(GluonWalletSavedData::new, GluonWalletSavedData::load), DATA_ID);
    }

    public synchronized long get(UUID playerId) {
        return Math.max(0L, balances.getOrDefault(playerId, 0L));
    }

    public synchronized long set(UUID playerId, long amount) {
        long normalized = Math.max(0L, amount);
        if (normalized == 0L) {
            balances.remove(playerId);
        } else {
            balances.put(playerId, normalized);
        }
        setDirty();
        return normalized;
    }

    public synchronized long add(UUID playerId, long amount) {
        if (amount <= 0L) {
            return get(playerId);
        }
        long current = get(playerId);
        long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        return set(playerId, next);
    }

    /**
     * Atomically subtracts {@code amount} if the balance is sufficient. Never goes negative.
     *
     * @return {@code true} if the debit was applied
     */
    public synchronized boolean trySubtract(UUID playerId, long amount) {
        if (amount < 0L) {
            return false;
        }
        if (amount == 0L) {
            return true;
        }
        long current = get(playerId);
        if (current < amount) {
            return false;
        }
        set(playerId, current - amount);
        return true;
    }

    private static GluonWalletSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        GluonWalletSavedData data = new GluonWalletSavedData();
        if (!tag.contains(PLAYERS_KEY, Tag.TAG_LIST)) {
            return data;
        }

        ListTag players = tag.getList(PLAYERS_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : players) {
            CompoundTag entry = (CompoundTag) entryTag;
            try {
                UUID playerId = entry.hasUUID("player_id")
                        ? entry.getUUID("player_id")
                        : UUID.fromString(entry.getString("player_id"));
                long amount = entry.getLong("gluons");
                if (amount > 0L) {
                    data.balances.put(playerId, amount);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            if (entry.getValue() <= 0L) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player_id", entry.getKey());
            playerTag.putLong("gluons", entry.getValue());
            players.add(playerTag);
        }
        tag.put(PLAYERS_KEY, players);
        return tag;
    }
}
