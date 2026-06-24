package com.mawlee.cointcore.punishment;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PunishmentHistorySavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_punishment_history";
    private static final String PLAYERS_KEY = "players";
    private static final int MAX_RECORDS_PER_PLAYER = 200;

    private final Map<UUID, List<PunishmentRecord>> recordsByPlayer = new HashMap<>();

    private PunishmentHistorySavedData() {
    }

    public static PunishmentHistorySavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(PunishmentHistorySavedData::new, PunishmentHistorySavedData::load), DATA_ID);
    }

    public void append(PunishmentRecord record) {
        List<PunishmentRecord> records = recordsByPlayer.computeIfAbsent(record.targetId(), ignored -> new ArrayList<>());
        records.add(record);
        trim(records);
        setDirty();
    }

    public List<PunishmentRecord> getRecords(UUID playerId, int limit) {
        List<PunishmentRecord> records = recordsByPlayer.get(playerId);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        int fromIndex = Math.max(0, records.size() - Math.max(1, limit));
        List<PunishmentRecord> slice = new ArrayList<>(records.subList(fromIndex, records.size()));
        Collections.reverse(slice);
        return slice;
    }

    public int count(UUID playerId) {
        List<PunishmentRecord> records = recordsByPlayer.get(playerId);
        return records == null ? 0 : records.size();
    }

    private static void trim(List<PunishmentRecord> records) {
        while (records.size() > MAX_RECORDS_PER_PLAYER) {
            records.removeFirst();
        }
    }

    private static PunishmentHistorySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PunishmentHistorySavedData data = new PunishmentHistorySavedData();
        if (!tag.contains(PLAYERS_KEY, Tag.TAG_COMPOUND)) {
            return data;
        }

        CompoundTag players = tag.getCompound(PLAYERS_KEY);
        for (String playerKey : players.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(playerKey);
                ListTag entries = players.getList(playerKey, Tag.TAG_COMPOUND);
                List<PunishmentRecord> records = new ArrayList<>();

                for (int i = 0; i < entries.size(); i++) {
                    PunishmentRecord record = readRecord(entries.getCompound(i), playerId);
                    if (record != null) {
                        records.add(record);
                    }
                }

                if (!records.isEmpty()) {
                    data.recordsByPlayer.put(playerId, records);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return data;
    }

    private static PunishmentRecord readRecord(CompoundTag entry, UUID fallbackTargetId) {
        try {
            PunishmentType type = PunishmentType.valueOf(entry.getString("type"));
            UUID targetId = entry.contains("targetId", Tag.TAG_STRING)
                    ? UUID.fromString(entry.getString("targetId"))
                    : fallbackTargetId;
            UUID id = entry.contains("id", Tag.TAG_STRING)
                    ? UUID.fromString(entry.getString("id"))
                    : UUID.randomUUID();

            return new PunishmentRecord(
                    id,
                    type,
                    targetId,
                    entry.getString("targetName"),
                    entry.getString("issuer"),
                    entry.getString("reason"),
                    entry.getLong("issuedAt"),
                    entry.getLong("expiresAt")
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag players = new CompoundTag();

        for (Map.Entry<UUID, List<PunishmentRecord>> entry : recordsByPlayer.entrySet()) {
            ListTag entries = new ListTag();
            for (PunishmentRecord record : entry.getValue()) {
                entries.add(writeRecord(record));
            }
            players.put(entry.getKey().toString(), entries);
        }

        tag.put(PLAYERS_KEY, players);
        return tag;
    }

    private static CompoundTag writeRecord(PunishmentRecord record) {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", record.id().toString());
        entry.putString("type", record.type().name());
        entry.putString("targetId", record.targetId().toString());
        entry.putString("targetName", record.targetName());
        entry.putString("issuer", record.issuer());
        entry.putString("reason", record.reason());
        entry.putLong("issuedAt", record.issuedAt());
        entry.putLong("expiresAt", record.expiresAt());
        return entry;
    }
}
