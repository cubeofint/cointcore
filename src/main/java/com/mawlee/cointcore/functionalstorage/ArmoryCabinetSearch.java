package com.mawlee.cointcore.functionalstorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Server-safe Armory Cabinet filter: hover name + item registry id, no tooltips.
 */
public final class ArmoryCabinetSearch {
    private ArmoryCabinetSearch() {
    }

    public static boolean matches(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String hoverName = stack.getHoverName().getString();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return matchesQuery(hoverName, id != null ? id.toString() : "", query);
    }

    /**
     * {@code query} is compared case-insensitively (Functional Storage already lowercases it).
     */
    public static boolean matchesQuery(String hoverName, String registryId, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        if (hoverName != null && hoverName.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return registryId != null && registryId.toLowerCase(Locale.ROOT).contains(needle);
    }
}
