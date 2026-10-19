package com.mawlee.cointcore.functionalstorage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Armory Cabinet filter that is identical on client and dedicated server.
 * Must not call {@code ItemStack#getTooltipLines} (Relics crash) and must not
 * use localized hover names (client language vs server language desyncs the
 * 32 visible slots after {@code broadcastChanges}).
 */
public final class ArmoryCabinetSearch {
    private ArmoryCabinetSearch() {
    }

    public static boolean matches(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String registryId = id != null ? id.toString() : "";
        String path = id != null ? id.getPath() : "";
        String descriptionId = stack.getItem().getDescriptionId();
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        String customName = custom != null ? custom.getString() : "";
        return matchesQuery(customName, registryId, path, descriptionId, query);
    }

    /**
     * {@code query} is compared case-insensitively (Functional Storage already lowercases it).
     */
    public static boolean matchesQuery(
            String customName,
            String registryId,
            String path,
            String descriptionId,
            String query
    ) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return contains(customName, needle)
                || contains(registryId, needle)
                || contains(path, needle)
                || contains(descriptionId, needle);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}
