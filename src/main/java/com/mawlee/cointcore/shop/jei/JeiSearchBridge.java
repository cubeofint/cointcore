package com.mawlee.cointcore.shop.jei;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashSet;
import java.util.Set;

/**
 * Optional JEI overlay filter access. Never references JEI types so the rest of
 * the mod still loads when JEI is absent.
 */
public final class JeiSearchBridge {
    public static final String JEI_MOD_ID = "jei";
    private static final String PLUGIN_CLASS = "com.mawlee.cointcore.shop.jei.CointJeiPlugin";

    private static String cachedText;
    private static Set<Item> cachedItems;

    private JeiSearchBridge() {
    }

    public static boolean available() {
        return FMLEnvironment.dist == Dist.CLIENT
                && ModList.get().isLoaded(JEI_MOD_ID)
                && filter() != null;
    }

    public static String getFilterText() {
        Object filter = filter();
        if (filter == null) {
            return null;
        }
        try {
            Object text = filter.getClass().getMethod("getFilterText").invoke(filter);
            return text == null ? "" : text.toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public static void setFilterText(String text) {
        Object filter = filter();
        if (filter == null) {
            return;
        }
        try {
            filter.getClass().getMethod("setFilterText", String.class).invoke(filter, text == null ? "" : text);
            cachedText = null;
            cachedItems = null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static Set<Item> filteredItems() {
        if (!available()) {
            return null;
        }
        String text = getFilterText();
        if (text != null && text.equals(cachedText) && cachedItems != null) {
            return cachedItems;
        }
        Object filter = filter();
        if (filter == null) {
            return null;
        }
        try {
            Object list = filter.getClass().getMethod("getFilteredItemStacks").invoke(filter);
            if (!(list instanceof Iterable<?> stacks)) {
                return Set.of();
            }
            Set<Item> items = new HashSet<>();
            for (Object entry : stacks) {
                if (entry instanceof ItemStack stack && !stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
            cachedText = text;
            cachedItems = items;
            return items;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object filter() {
        Object runtime = runtime();
        if (runtime == null) {
            return null;
        }
        try {
            return runtime.getClass().getMethod("getIngredientFilter").invoke(runtime);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object runtime() {
        if (FMLEnvironment.dist != Dist.CLIENT || !ModList.get().isLoaded(JEI_MOD_ID)) {
            return null;
        }
        try {
            return Class.forName(PLUGIN_CLASS).getMethod("runtime").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError | ExceptionInInitializerError ignored) {
            return null;
        }
    }
}
