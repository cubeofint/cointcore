package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World save of per-player completed rank quests ({@code cointcore_quest_ranks}).
 */
public final class QuestRankSavedData extends SavedData {
    static final String DATA_ID = CointCore.MOD_ID + "_quest_ranks";
    private static final String PLAYERS_KEY = "players";

    private final QuestRankCompletionStore store = new QuestRankCompletionStore();

    private QuestRankSavedData() {
    }

    public static QuestRankSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(QuestRankSavedData::new, QuestRankSavedData::load), DATA_ID);
    }

    public boolean record(UUID playerId, long questId) {
        boolean added = store.record(playerId, questId);
        if (added) {
            setDirty();
        }
        return added;
    }

    public int forget(UUID playerId, Set<Long> questIds) {
        int removed = store.forget(playerId, questIds);
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public Set<Long> completed(UUID playerId) {
        return store.completed(playerId);
    }

    public boolean has(UUID playerId, long questId) {
        return store.has(playerId, questId);
    }

    QuestRankCompletionStore store() {
        return store;
    }

    private static QuestRankSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        QuestRankSavedData data = new QuestRankSavedData();
        if (!tag.contains(PLAYERS_KEY, Tag.TAG_LIST)) {
            return data;
        }
        Map<UUID, Set<Long>> loaded = new HashMap<>();
        ListTag players = tag.getList(PLAYERS_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : players) {
            if (!(entryTag instanceof CompoundTag entry)) {
                continue;
            }
            try {
                UUID playerId = entry.hasUUID("player_id")
                        ? entry.getUUID("player_id")
                        : UUID.fromString(entry.getString("player_id"));
                Set<Long> quests = new LinkedHashSet<>();
                ListTag questTags = entry.getList("quests", Tag.TAG_STRING);
                for (Tag questTag : questTags) {
                    QuestRankIds.parseHex(questTag.getAsString()).ifPresent(quests::add);
                }
                if (!quests.isEmpty()) {
                    loaded.put(playerId, quests);
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
        for (Map.Entry<UUID, Set<Long>> entry : store.snapshot().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player_id", entry.getKey());
            ListTag quests = new ListTag();
            for (long questId : entry.getValue()) {
                quests.add(StringTag.valueOf(QuestRankIds.toHex(questId)));
            }
            playerTag.put("quests", quests);
            players.add(playerTag);
        }
        tag.put(PLAYERS_KEY, players);
        return tag;
    }
}
