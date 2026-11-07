package com.mawlee.cointcore.modularbees;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Everything a ModularBees core hands an RS-connected export hatch, kept per resource with no size limit so the
 * machine never waits for the next RS batch. Flushed into storage on the hatch's schedule.
 */
public final class ExportBuffer {
    private static final String TAG = "cointcore_rs_buffer";
    private final Object2LongLinkedOpenHashMap<AEKey> amounts = new Object2LongLinkedOpenHashMap<>();

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    public void add(AEKey key, long amount) {
        if (key != null && amount > 0) amounts.addTo(key, amount);
    }

    public long amount(AEKey key) {
        return amounts.getLong(key);
    }

    /** Inserts as much as the storage takes; returns true if anything moved. */
    public boolean flush(MEStorage storage, IActionSource source) {
        if (storage == null || amounts.isEmpty()) return false;
        boolean moved = false;
        var iterator = amounts.object2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long inserted = storage.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE, source);
            if (inserted <= 0) continue;
            moved = true;
            long left = entry.getLongValue() - inserted;
            if (left <= 0) iterator.remove();
            else entry.setValue(left);
        }
        return moved;
    }

    /** Buffered items as normal-sized stacks, then cleared (fluids cannot be dropped and are lost). */
    public void drainItemsInto(List<ItemStack> drops) {
        for (var entry : amounts.object2LongEntrySet()) {
            if (!(entry.getKey() instanceof AEItemKey item)) continue;
            long left = entry.getLongValue();
            int max = Math.max(1, item.getMaxStackSize());
            while (left > 0) {
                int count = (int) Math.min(max, left);
                drops.add(item.toStack(count));
                left -= count;
            }
        }
        amounts.clear();
    }

    public void save(CompoundTag data, HolderLookup.Provider provider) {
        if (amounts.isEmpty()) {
            data.remove(TAG);
            return;
        }
        ListTag list = new ListTag();
        for (var entry : amounts.object2LongEntrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put("key", entry.getKey().toTagGeneric(provider));
            tag.putLong("amount", entry.getLongValue());
            list.add(tag);
        }
        data.put(TAG, list);
    }

    public void load(CompoundTag data, HolderLookup.Provider provider) {
        amounts.clear();
        for (Tag raw : data.getList(TAG, Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            AEKey key = AEKey.fromTagGeneric(provider, tag.getCompound("key"));
            add(key, tag.getLong("amount"));
        }
    }
}
