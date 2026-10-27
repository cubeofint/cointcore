package com.mawlee.cointcore.refinedstorage.wireless;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class MagnetState {
    private static final String KEY = "cointcore_wireless_magnet";
    private MagnetState() { }

    public static boolean accepts(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.equals("cointcore:wireless_magnet_card") || id.equals("ae2wtlib:magnet_card");
    }

    private static CompoundTag state(ItemStack device) {
        return device.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
    }

    public static boolean installed(ItemStack device) { return state(device).contains("card", net.minecraft.nbt.Tag.TAG_COMPOUND); }
    public static MagnetMode mode(ItemStack device) { return installed(device) ? MagnetMode.fromId(state(device).getInt("mode")) : MagnetMode.OFF; }
    public static ItemStack card(ItemStack device, HolderLookup.Provider registries) {
        return ItemStack.parseOptional(registries, state(device).getCompound("card"));
    }

    public static void save(ItemStack device, ItemStack card, MagnetMode mode, HolderLookup.Provider registries) {
        var state = new CompoundTag();
        if (!card.isEmpty()) state.put("card", card.copyWithCount(1).save(registries));
        state.putInt("mode", card.isEmpty() ? 0 : mode.ordinal());
        CustomData.update(DataComponents.CUSTOM_DATA, device, tag -> tag.put(KEY, state));
    }

    public static void cycle(ItemStack device) {
        if (!installed(device)) return;
        MagnetMode next = mode(device).next();
        CustomData.update(DataComponents.CUSTOM_DATA, device, tag -> tag.getCompound(KEY).putInt("mode", next.ordinal()));
    }
}
