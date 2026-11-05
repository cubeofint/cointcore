package com.mawlee.cointcore.twilight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Twilight Forest boss arenas: where each boss spawner stood, which spawner block it was, and the bosses from it that
 * are still alive (loaded or not). An arena whose bosses are all dead gets its spawner back on the next start.
 */
public final class TwilightBossSavedData extends SavedData {
    private static final String DATA_ID = "cointcore_twilight_bosses";

    /** One boss arena. {@code alive} holds the UUIDs of bosses from it that have not died or been discarded. */
    public static final class Arena {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        ResourceLocation spawner;
        final Set<UUID> alive = new LinkedHashSet<>();

        Arena(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation spawner) {
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.spawner = spawner;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public BlockPos pos() {
            return pos;
        }

        public ResourceLocation spawner() {
            return spawner;
        }

        public int aliveCount() {
            return alive.size();
        }
    }

    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    private TwilightBossSavedData() {
    }

    public static TwilightBossSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TwilightBossSavedData::new, TwilightBossSavedData::load), DATA_ID);
    }

    private static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.asLong();
    }

    public List<Arena> arenas() {
        return Collections.unmodifiableList(new ArrayList<>(arenas.values()));
    }

    /** Records a boss from the spawner at {@code pos}; returns true if this arena was new. */
    public boolean bossJoined(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation spawner, UUID boss) {
        String key = key(dimension, pos);
        Arena arena = arenas.get(key);
        boolean created = arena == null;
        if (created) {
            arena = new Arena(dimension, pos, spawner);
            arenas.put(key, arena);
        } else {
            arena.spawner = spawner;
        }
        if (arena.alive.add(boss) || created) {
            setDirty();
        }
        return created;
    }

    /** The boss died or was discarded: forget it in every arena. */
    public void bossGone(UUID boss) {
        for (Arena arena : arenas.values()) {
            if (arena.alive.remove(boss)) {
                setDirty();
            }
        }
    }

    /** Adds an arena by hand (for bosses killed before this was tracked); false if one is already there. */
    public boolean mark(ResourceKey<Level> dimension, BlockPos pos, ResourceLocation spawner) {
        String key = key(dimension, pos);
        if (arenas.containsKey(key)) {
            return false;
        }
        arenas.put(key, new Arena(dimension, pos, spawner));
        setDirty();
        return true;
    }

    /** The spawner had to go elsewhere (a loot chest took its spot): the arena follows it, keeping its place in the list. */
    void move(Arena arena, BlockPos pos) {
        Map<String, Arena> reordered = new LinkedHashMap<>();
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            if (entry.getValue() == arena) {
                Arena moved = new Arena(arena.dimension, pos, arena.spawner);
                moved.alive.addAll(arena.alive);
                reordered.putIfAbsent(key(arena.dimension, pos), moved);
            } else {
                reordered.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        arenas.clear();
        arenas.putAll(reordered);
        setDirty();
    }

    public Arena remove(int index) {
        if (index < 0 || index >= arenas.size()) {
            return null;
        }
        Arena arena = new ArrayList<>(arenas.values()).get(index);
        arenas.remove(key(arena.dimension, arena.pos));
        setDirty();
        return arena;
    }

    /** Forgets the bosses an arena thinks are alive, so it respawns even if one vanished without a trace. */
    public boolean forgetAlive(int index) {
        if (index < 0 || index >= arenas.size()) {
            return false;
        }
        new ArrayList<>(arenas.values()).get(index).alive.clear();
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Arena arena : arenas.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("dimension", arena.dimension.location().toString());
            entry.put("pos", NbtUtils.writeBlockPos(arena.pos));
            entry.putString("spawner", arena.spawner.toString());
            ListTag alive = new ListTag();
            for (UUID id : arena.alive) {
                alive.add(NbtUtils.createUUID(id));
            }
            entry.put("alive", alive);
            list.add(entry);
        }
        tag.put("arenas", list);
        return tag;
    }

    private static TwilightBossSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TwilightBossSavedData data = new TwilightBossSavedData();
        for (Tag raw : tag.getList("arenas", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            ResourceLocation spawner = ResourceLocation.tryParse(entry.getString("spawner"));
            BlockPos pos = NbtUtils.readBlockPos(entry, "pos").orElse(null);
            if (dimension == null || spawner == null || pos == null) {
                continue;
            }
            Arena arena = new Arena(ResourceKey.create(Registries.DIMENSION, dimension), pos, spawner);
            for (Tag id : entry.getList("alive", Tag.TAG_INT_ARRAY)) {
                arena.alive.add(NbtUtils.loadUUID(id));
            }
            data.arenas.put(key(arena.dimension, arena.pos), arena);
        }
        return data;
    }
}
