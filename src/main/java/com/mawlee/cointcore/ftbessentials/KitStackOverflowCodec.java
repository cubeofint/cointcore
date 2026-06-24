package com.mawlee.cointcore.ftbessentials;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class KitStackOverflowCodec {
    public static final String KIT_COUNT_KEY = "cointcore:kit_count";
    private static final int MAX_SAVE_COUNT = 99;

    private KitStackOverflowCodec() {
    }

    public static SNBTCompoundTag encodeStack(ItemStack stack, HolderLookup.Provider provider) {
        int realCount = stack.getCount();
        ItemStack toSave = realCount > MAX_SAVE_COUNT ? stack.copyWithCount(MAX_SAVE_COUNT) : stack;
        CompoundTag saved = (CompoundTag) toSave.save(provider);
        SNBTCompoundTag tag = SNBTCompoundTag.of(saved);

        if (realCount > MAX_SAVE_COUNT) {
            tag.putInt(KIT_COUNT_KEY, realCount);
        }

        normalizeNestedItemTags(tag);
        return tag;
    }

    public static Optional<ItemStack> decodeStack(HolderLookup.Provider provider, CompoundTag tag) {
        CompoundTag normalized = tag.copy();
        normalizeNestedItemTags(normalized);

        Optional<ItemStack> parsed = ItemStack.parse(provider, normalized);
        parsed.ifPresent(stack -> restoreStackCounts(stack, tag, provider));
        return parsed;
    }

    static void normalizeNestedItemTags(CompoundTag tag) {
        walkTag(tag);
    }

    private static void walkTag(Tag tag) {
        switch (tag) {
            case CompoundTag compound -> {
                normalizeItemStackTag(compound);
                for (String key : compound.getAllKeys()) {
                    walkTag(compound.get(key));
                }
            }
            case ListTag list -> {
                for (Tag element : list) {
                    walkTag(element);
                }
            }
            default -> {
            }
        }
    }

    private static void normalizeItemStackTag(CompoundTag tag) {
        if (!isItemStackTag(tag)) {
            return;
        }

        int count = readCount(tag);
        if (count <= MAX_SAVE_COUNT) {
            return;
        }

        tag.putInt(KIT_COUNT_KEY, count);
        writeCount(tag, MAX_SAVE_COUNT);
    }

    private static void restoreStackCounts(ItemStack stack, CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains(KIT_COUNT_KEY)) {
            stack.setCount(tag.getInt(KIT_COUNT_KEY));
        }

        if (!tag.contains("components", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag components = tag.getCompound("components");
        restoreContainerContents(stack, components, provider);
        restoreBundleContents(stack, components, provider);
    }

    private static void restoreContainerContents(ItemStack stack, CompoundTag components, HolderLookup.Provider provider) {
        if (!components.contains("minecraft:container", Tag.TAG_LIST)) {
            return;
        }

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        ListTag slots = components.getList("minecraft:container", Tag.TAG_COMPOUND);
        NonNullList<ItemStack> items = NonNullList.create();
        contents.copyInto(items);
        SlotHolder holder = new SlotHolder(items);

        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slotTag = slots.getCompound(i);
            int slot = slotTag.getInt("slot");
            if (!slotTag.contains("item", Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag itemTag = slotTag.getCompound("item");
            ItemStack slotStack = holder.get(slot);
            if (slotStack.isEmpty()) {
                decodeStack(provider, itemTag).ifPresent(restored -> holder.set(slot, restored));
            } else {
                restoreStackCounts(slotStack, itemTag, provider);
            }
        }

        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(holder.items()));
    }

    private static void restoreBundleContents(ItemStack stack, CompoundTag components, HolderLookup.Provider provider) {
        if (!components.contains("minecraft:bundle_contents", Tag.TAG_LIST)) {
            return;
        }

        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle == null) {
            return;
        }

        ListTag bundleItems = components.getList("minecraft:bundle_contents", Tag.TAG_COMPOUND);
        List<ItemStack> items = new ArrayList<>();
        bundle.items().forEach(item -> items.add(item.copy()));
        int index = 0;

        for (int i = 0; i < bundleItems.size(); i++) {
            CompoundTag itemTag = bundleItems.getCompound(i);
            while (index < items.size() && items.get(index).isEmpty()) {
                index++;
            }
            if (index >= items.size()) {
                break;
            }

            final int slotIndex = index;
            ItemStack slotStack = items.get(slotIndex);
            if (slotStack.isEmpty()) {
                decodeStack(provider, itemTag).ifPresent(restored -> items.set(slotIndex, restored));
            } else {
                restoreStackCounts(slotStack, itemTag, provider);
            }
            index++;
        }

        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
    }

    private static boolean isItemStackTag(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING) && tag.contains("count");
    }

    private static int readCount(CompoundTag tag) {
        if (tag.contains("count", Tag.TAG_INT)) {
            return tag.getInt("count");
        }
        if (tag.contains("count", Tag.TAG_BYTE)) {
            return tag.getByte("count");
        }
        return 1;
    }

    private static void writeCount(CompoundTag tag, int count) {
        tag.putInt("count", count);
    }

    private static final class SlotHolder {
        private final NonNullList<ItemStack> items;

        private SlotHolder(NonNullList<ItemStack> items) {
            this.items = items;
        }

        ItemStack get(int slot) {
            if (slot < 0 || slot >= items.size()) {
                return ItemStack.EMPTY;
            }
            return items.get(slot);
        }

        void set(int slot, ItemStack stack) {
            while (items.size() <= slot) {
                items.add(ItemStack.EMPTY);
            }
            items.set(slot, stack);
        }

        NonNullList<ItemStack> items() {
            return items;
        }
    }
}
