package com.mawlee.cointcore.ghost;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ghost hunt: the ghosts staff marked (a block position or an entity) and which ghosts each player found.
 * Finds of a removed ghost no longer count.
 */
public final class GhostHuntSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_ghost_hunt";

    public enum Kind {
        BLOCK,
        ENTITY
    }

    /** {@code pos} for blocks, {@code entityId} for entities. */
    public record Ghost(int id, Kind kind, String dimension, long pos, UUID entityId, String label) {
    }

    private final Map<Integer, Ghost> ghosts = new LinkedHashMap<>();
    private final Map<String, Integer> byBlock = new HashMap<>();
    private final Map<UUID, Integer> byEntity = new HashMap<>();
    private final Map<UUID, Set<Integer>> finds = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private int nextId = 1;

    private GhostHuntSavedData() {
    }

    public static GhostHuntSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(GhostHuntSavedData::new, GhostHuntSavedData::load), DATA_ID);
    }

    private static String blockKey(String dimension, long pos) {
        return dimension + "|" + pos;
    }

    public synchronized Optional<Ghost> atBlock(String dimension, long pos) {
        Integer id = byBlock.get(blockKey(dimension, pos));
        return id == null ? Optional.empty() : Optional.ofNullable(ghosts.get(id));
    }

    public synchronized Optional<Ghost> ofEntity(UUID entityId) {
        Integer id = byEntity.get(entityId);
        return id == null ? Optional.empty() : Optional.ofNullable(ghosts.get(id));
    }

    public synchronized Optional<Ghost> byId(int id) {
        return Optional.ofNullable(ghosts.get(id));
    }

    public synchronized Collection<Ghost> all() {
        return List.copyOf(ghosts.values());
    }

    public synchronized int total() {
        return ghosts.size();
    }

    public synchronized Ghost addBlock(String dimension, long pos, String label) {
        Ghost ghost = new Ghost(nextId++, Kind.BLOCK, dimension, pos, null, label);
        ghosts.put(ghost.id(), ghost);
        byBlock.put(blockKey(dimension, pos), ghost.id());
        setDirty();
        return ghost;
    }

    public synchronized Ghost addEntity(String dimension, long pos, UUID entityId, String label) {
        Ghost ghost = new Ghost(nextId++, Kind.ENTITY, dimension, pos, entityId, label);
        ghosts.put(ghost.id(), ghost);
        byEntity.put(entityId, ghost.id());
        setDirty();
        return ghost;
    }

    public synchronized boolean remove(int id) {
        Ghost ghost = ghosts.remove(id);
        if (ghost == null) {
            return false;
        }
        if (ghost.kind() == Kind.BLOCK) {
            byBlock.remove(blockKey(ghost.dimension(), ghost.pos()));
        } else {
            byEntity.remove(ghost.entityId());
        }
        finds.values().forEach(set -> set.remove(id));
        setDirty();
        return true;
    }

    /** Marks a find; false when this player already found it. */
    public synchronized boolean markFound(UUID playerId, String playerName, int ghostId) {
        names.put(playerId, playerName);
        boolean added = finds.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(ghostId);
        if (added) {
            setDirty();
        }
        return added;
    }

    public synchronized int foundCount(UUID playerId) {
        Set<Integer> set = finds.get(playerId);
        if (set == null) {
            return 0;
        }
        int count = 0;
        for (int id : set) {
            if (ghosts.containsKey(id)) {
                count++;
            }
        }
        return count;
    }

    public synchronized boolean hasFound(UUID playerId, int ghostId) {
        Set<Integer> set = finds.get(playerId);
        return set != null && set.contains(ghostId);
    }

    public synchronized String nameOf(UUID playerId) {
        return names.getOrDefault(playerId, playerId.toString().substring(0, 8));
    }

    /** Players by found count, most first. */
    public synchronized List<Map.Entry<UUID, Integer>> ranking() {
        List<Map.Entry<UUID, Integer>> rows = new ArrayList<>();
        for (UUID playerId : finds.keySet()) {
            int count = foundCount(playerId);
            if (count > 0) {
                rows.add(Map.entry(playerId, count));
            }
        }
        rows.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());
        return rows;
    }

    public synchronized void resetFinds() {
        finds.clear();
        setDirty();
    }

    private static GhostHuntSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        GhostHuntSavedData data = new GhostHuntSavedData();
        data.nextId = Math.max(1, tag.getInt("next_id"));
        for (Tag raw : tag.getList("ghosts", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            int id = entry.getInt("id");
            Kind kind = "ENTITY".equals(entry.getString("kind")) ? Kind.ENTITY : Kind.BLOCK;
            UUID entityId = entry.hasUUID("entity") ? entry.getUUID("entity") : null;
            if (kind == Kind.ENTITY && entityId == null) {
                continue;
            }
            Ghost ghost = new Ghost(id, kind, entry.getString("dim"), entry.getLong("pos"), entityId, entry.getString("label"));
            data.ghosts.put(id, ghost);
            if (kind == Kind.BLOCK) {
                data.byBlock.put(blockKey(ghost.dimension(), ghost.pos()), id);
            } else {
                data.byEntity.put(entityId, id);
            }
            data.nextId = Math.max(data.nextId, id + 1);
        }
        for (Tag raw : tag.getList("finds", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID("player")) {
                continue;
            }
            UUID playerId = entry.getUUID("player");
            Set<Integer> found = new HashSet<>();
            for (int id : entry.getIntArray("ghosts")) {
                found.add(id);
            }
            data.finds.put(playerId, found);
            if (entry.contains("name")) {
                data.names.put(playerId, entry.getString("name"));
            }
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("next_id", nextId);
        ListTag ghostList = new ListTag();
        for (Ghost ghost : ghosts.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", ghost.id());
            entry.putString("kind", ghost.kind().name());
            entry.putString("dim", ghost.dimension());
            entry.putLong("pos", ghost.pos());
            if (ghost.entityId() != null) {
                entry.putUUID("entity", ghost.entityId());
            }
            entry.putString("label", ghost.label() == null ? "" : ghost.label());
            ghostList.add(entry);
        }
        tag.put("ghosts", ghostList);
        ListTag findList = new ListTag();
        for (Map.Entry<UUID, Set<Integer>> entry : finds.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", entry.getKey());
            row.put("ghosts", new IntArrayTag(entry.getValue().stream().mapToInt(Integer::intValue).toArray()));
            String name = names.get(entry.getKey());
            if (name != null) {
                row.putString("name", name);
            }
            findList.add(row);
        }
        tag.put("finds", findList);
        return tag;
    }
}
