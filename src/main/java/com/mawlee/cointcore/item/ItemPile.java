package com.mawlee.cointcore.item;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Hidden reserve of an item pile entity: aggregated item counts behind the one stack it displays.
 */
public final class ItemPile {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";

    private final List<Entry> entries;
    private long total;

    public ItemPile(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
        for (Entry entry : this.entries) {
            total += entry.count;
        }
    }

    public static final class Entry {
        private final ItemStack proto;
        private long count;

        public Entry(ItemStack proto, long count) {
            this.proto = proto.copyWithCount(1);
            this.count = count;
        }

        public ItemStack proto() {
            return proto;
        }

        public long count() {
            return count;
        }

        void grow(long amount) {
            count += amount;
        }
    }

    public boolean isEmpty() {
        return total <= 0L;
    }

    public long total() {
        return total;
    }

    /** Removes up to one full stack of the first entry. */
    public ItemStack takeChunk() {
        while (!entries.isEmpty()) {
            Entry entry = entries.getFirst();
            if (entry.count <= 0L) {
                entries.removeFirst();
                continue;
            }
            int size = (int) Math.min(entry.count, Math.max(1, entry.proto.getMaxStackSize()));
            entry.count -= size;
            total -= size;
            if (entry.count <= 0L) {
                entries.removeFirst();
            }
            return entry.proto.copyWithCount(size);
        }
        return ItemStack.EMPTY;
    }

    /**
     * Refills a partially taken display stack from the matching reserve entry.
     *
     * @return items moved into {@code display}
     */
    public int topUp(ItemStack display) {
        int space = display.getMaxStackSize() - display.getCount();
        if (space <= 0 || display.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (!ItemStack.isSameItemSameComponents(entry.proto, display)) {
                continue;
            }
            int moved = (int) Math.min(space, entry.count);
            entry.count -= moved;
            total -= moved;
            if (entry.count <= 0L) {
                entries.remove(i);
            }
            display.grow(moved);
            return moved;
        }
        return 0;
    }

    public static CompoundTag encodeEntry(HolderLookup.Provider registries, Entry entry) {
        CompoundTag tag = new CompoundTag();
        tag.put(STACK_KEY, entry.proto.save(registries));
        tag.putLong(COUNT_KEY, entry.count);
        return tag;
    }

    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            if (entry.count > 0L) {
                list.add(encodeEntry(registries, entry));
            }
        }
        return list;
    }

    public static ItemPile load(HolderLookup.Provider registries, ListTag list) {
        List<Entry> loaded = new ArrayList<>(list.size());
        long dropped = 0L;
        for (Tag tag : list) {
            if (!(tag instanceof CompoundTag entryTag)) {
                continue;
            }
            long count = entryTag.getLong(COUNT_KEY);
            if (count <= 0L) {
                continue;
            }
            ItemStack proto = ItemStack.parse(registries, entryTag.getCompound(STACK_KEY)).orElse(ItemStack.EMPTY);
            if (proto.isEmpty()) {
                dropped += count;
                continue;
            }
            loaded.add(new Entry(proto, count));
        }
        if (dropped > 0L) {
            LOGGER.warn("Item pile lost {} items of unknown types while loading", dropped);
        }
        return new ItemPile(loaded);
    }
}
