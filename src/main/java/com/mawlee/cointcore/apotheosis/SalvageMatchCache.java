package com.mawlee.cointcore.apotheosis;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-thread cache for Apotheosis salvaging recipe lookups, keyed by item + component patch (salvage recipes match
 * on the item and its affix rarity, never on the count). Cleared when the recipe manager is replaced by a reload.
 */
public final class SalvageMatchCache {
    private static final int MAX_ENTRIES = 512;

    private record Key(Item item, DataComponentPatch patch) {
    }

    private static final Map<Key, List<?>> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, List<?>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static RecipeManager owner;

    private SalvageMatchCache() {
    }

    /** Cached result for this stack, or null when it has to be computed. */
    public static List<?> get(RecipeManager recipes, ItemStack stack) {
        if (recipes != owner) {
            CACHE.clear();
            owner = recipes;
            return null;
        }
        return CACHE.get(new Key(stack.getItem(), stack.getComponentsPatch()));
    }

    public static void put(RecipeManager recipes, ItemStack stack, List<?> result) {
        if (recipes != owner || result == null) {
            return;
        }
        CACHE.put(new Key(stack.getItem(), stack.getComponentsPatch()), List.copyOf(result));
    }

    public static void clear() {
        CACHE.clear();
        owner = null;
    }
}
