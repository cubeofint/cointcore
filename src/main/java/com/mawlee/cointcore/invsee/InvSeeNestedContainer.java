package com.mawlee.cointcore.invsee;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Live view of nested item contents. Writes go back onto the parent stack.
 */
public final class InvSeeNestedContainer implements Container {
    private final ItemStack parent;
    private final List<ItemStack> slots;
    private final boolean editable;

    public InvSeeNestedContainer(ItemStack parent, boolean editable) {
        this.parent = parent;
        this.editable = editable;
        List<ItemStack> viewed = InvSeeItemContents.view(parent);
        int size = Math.max(InvSeeItemContents.slotCount(parent), viewed.size());
        size = Math.min(InvSeeItemContents.MAX_SLOTS, Math.max(size, 27));
        this.slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (i < viewed.size()) {
                slots.add(viewed.get(i));
            } else {
                slots.add(ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int getContainerSize() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return ItemStack.EMPTY;
        }
        return slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || !editable) {
            return ItemStack.EMPTY;
        }
        ItemStack split = current.split(amount);
        if (current.isEmpty()) {
            slots.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (!editable) {
            return ItemStack.EMPTY;
        }
        slots.set(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots.size() || !editable) {
            return;
        }
        slots.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        if (editable) {
            InvSeeItemContents.write(parent, slots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        if (!editable) {
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }
}
