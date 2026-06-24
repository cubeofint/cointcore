package com.mawlee.cointcore.keepinventory.integrations;

import com.mawlee.cointcore.keepinventory.KeepInventoryCaptureProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosKeepInventoryCapture implements KeepInventoryCaptureProvider {
    public static final String ID = "curios";
    private static final String INVENTORY = "Inventory";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void capture(ServerPlayer player, CompoundTag root) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            CompoundTag section = new CompoundTag();
            section.put(INVENTORY, handler.saveInventory(true));
            root.put(ID, section);
        });
    }

    @Override
    public void restore(ServerPlayer player, CompoundTag root) {
        if (!root.contains(ID)) {
            return;
        }

        CompoundTag section = root.getCompound(ID);
        if (!section.contains(INVENTORY, ListTag.TAG_LIST)) {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.loadInventory(section.getList(INVENTORY, ListTag.TAG_COMPOUND));
        });
    }
}
