package com.mawlee.cointcore.invsee;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class InvSeeAttachmentItems {
    private static final int MAX_SLOTS = 54;

    private InvSeeAttachmentItems() {
    }

    public static List<ItemStack> extractItems(CompoundTag attachmentData, HolderLookup.Provider registries) {
        List<ItemStack> items = new ArrayList<>();
        collectItems(attachmentData, registries, items);
        if (items.size() > MAX_SLOTS) {
            return items.subList(0, MAX_SLOTS);
        }
        return items;
    }

    private static void collectItems(CompoundTag tag, HolderLookup.Provider registries, List<ItemStack> out) {
        if (out.size() >= MAX_SLOTS) {
            return;
        }

        if (looksLikeItemStack(tag)) {
            ItemStack stack = ItemStack.parseOptional(registries, tag);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
            return;
        }

        for (String key : tag.getAllKeys()) {
            if (out.size() >= MAX_SLOTS) {
                return;
            }
            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound) {
                collectItems(compound, registries, out);
            } else if (value instanceof ListTag list) {
                for (Tag element : list) {
                    if (element instanceof CompoundTag compound) {
                        collectItems(compound, registries, out);
                    }
                }
            }
        }
    }

    private static boolean looksLikeItemStack(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING) && tag.contains("count", Tag.TAG_INT);
    }
}
