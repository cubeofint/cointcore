package com.mawlee.cointcore.ars;

import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Avoids repeated {@link RecipeManager#getAllRecipesFor(RecipeType)} on every Crush resolve.
 */
public final class CrushRecipeCache {
    private static RecipeManager lastManager;
    private static RecipeType<?> lastType;
    private static List<?> cached = List.of();

    private CrushRecipeCache() {
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getAll(RecipeManager manager, RecipeType<?> type) {
        if (!ArsGlyphPerfConfig.isCrushCacheRecipes()) {
            return (List<T>) manager.getAllRecipesFor(unchecked(type));
        }
        if (manager != lastManager || type != lastType || cached == null) {
            cached = manager.getAllRecipesFor(unchecked(type));
            lastManager = manager;
            lastType = type;
        }
        return (List<T>) cached;
    }

    public static void invalidate() {
        lastManager = null;
        lastType = null;
        cached = List.of();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RecipeType unchecked(RecipeType<?> type) {
        return type;
    }
}
