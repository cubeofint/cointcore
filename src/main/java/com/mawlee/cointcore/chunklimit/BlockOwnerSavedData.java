package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Overworld-stored owners of player-placed limited blocks for all dimensions. */
public final class BlockOwnerSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_block_owners";
    private static final String ENTRIES_KEY = "entries";

    private final BlockOwnerRegistry registry = new BlockOwnerRegistry();

    private BlockOwnerSavedData() {
    }

    public static BlockOwnerSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(BlockOwnerSavedData::new, BlockOwnerSavedData::load), DATA_ID);
    }

    public BlockOwnerRegistry registry() {
        return registry;
    }

    public static long chunkOf(long pos) {
        return ChunkPos.asLong(
                SectionPos.blockToSectionCoord(BlockPos.getX(pos)),
                SectionPos.blockToSectionCoord(BlockPos.getZ(pos))
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        registry.forEach((dimension, pos, entry) -> {
            CompoundTag item = new CompoundTag();
            item.putString("d", dimension);
            item.putLong("p", pos);
            item.putUUID("o", entry.owner());
            item.putString("b", entry.blockId());
            list.add(item);
        });
        tag.put(ENTRIES_KEY, list);
        return tag;
    }

    private static BlockOwnerSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        BlockOwnerSavedData data = new BlockOwnerSavedData();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (!item.hasUUID("o")) {
                continue;
            }
            long pos = item.getLong("p");
            data.registry.put(item.getString("d"), chunkOf(pos), pos, item.getUUID("o"), item.getString("b"));
        }
        return data;
    }
}
