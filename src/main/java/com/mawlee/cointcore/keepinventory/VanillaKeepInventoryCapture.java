package com.mawlee.cointcore.keepinventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

final class VanillaKeepInventoryCapture implements KeepInventoryCaptureProvider {
    static final String ID = "vanilla";
    private static final String INVENTORY = "Inventory";
    private static final String SELECTED_SLOT = "SelectedItemSlot";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void capture(ServerPlayer player, CompoundTag root) {
        CompoundTag section = new CompoundTag();
        section.put(INVENTORY, player.getInventory().save(new ListTag()));
        section.putInt(SELECTED_SLOT, player.getInventory().selected);
        root.put(ID, section);
    }

    @Override
    public void restore(ServerPlayer player, CompoundTag root) {
        if (!root.contains(ID)) {
            return;
        }

        CompoundTag section = root.getCompound(ID);
        Inventory inventory = player.getInventory();
        if (section.contains(INVENTORY, ListTag.TAG_LIST)) {
            inventory.load(section.getList(INVENTORY, ListTag.TAG_COMPOUND));
        }
        if (section.contains(SELECTED_SLOT)) {
            inventory.selected = section.getInt(SELECTED_SLOT);
        }
    }
}
