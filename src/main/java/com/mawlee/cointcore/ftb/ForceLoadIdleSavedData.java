package com.mawlee.cointcore.ftb;

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
 * Whether each player held {@code cointcore.forceload.extended} the last time they were online. Permissions
 * can only be resolved for online players, so the flag is captured on login/logout and read while offline.
 * Players never seen since this was installed are "unknown" and their teams are left to FTB's own window.
 */
public final class ForceLoadIdleSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_forceload_idle";
    private static final String KNOWN_KEY = "known";
    private static final String EXTENDED_KEY = "extended";

    private final Set<UUID> known = new HashSet<>();
    private final Set<UUID> extended = new HashSet<>();

    private ForceLoadIdleSavedData() {
    }

    public static ForceLoadIdleSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(ForceLoadIdleSavedData::new, ForceLoadIdleSavedData::load), DATA_ID);
    }

    public boolean isKnown(UUID playerId) {
        return known.contains(playerId);
    }

    public boolean isExtended(UUID playerId) {
        return extended.contains(playerId);
    }

    public void record(UUID playerId, boolean isExtended) {
        boolean changed = known.add(playerId);
        changed |= isExtended ? extended.add(playerId) : extended.remove(playerId);
        if (changed) {
            setDirty();
        }
    }

    private static ForceLoadIdleSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ForceLoadIdleSavedData data = new ForceLoadIdleSavedData();
        readUuids(tag.getList(KNOWN_KEY, Tag.TAG_STRING), data.known);
        readUuids(tag.getList(EXTENDED_KEY, Tag.TAG_STRING), data.extended);
        return data;
    }

    private static void readUuids(ListTag list, Set<UUID> into) {
        for (int i = 0; i < list.size(); i++) {
            try {
                into.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static ListTag writeUuids(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put(KNOWN_KEY, writeUuids(known));
        tag.put(EXTENDED_KEY, writeUuids(extended));
        return tag;
    }
}
