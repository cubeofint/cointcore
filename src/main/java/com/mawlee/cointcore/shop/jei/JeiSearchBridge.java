package com.mawlee.cointcore.shop.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Optional JEI overlay filter access. Never references JEI types so the rest of
 * the mod still loads when JEI is absent.
 */
public final class JeiSearchBridge {
    public static final String JEI_MOD_ID = "jei";
    private static final String PLUGIN_CLASS = "com.mawlee.cointcore.shop.jei.CointJeiPlugin";

    private JeiSearchBridge() {
    }

    public static boolean available() {
        return FMLEnvironment.dist == Dist.CLIENT
                && ModList.get().isLoaded(JEI_MOD_ID)
                && runtime() != null;
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
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /**
     * {@code true}/{@code false} when JEI filtered the current query; {@code null} to fall back.
     */
    public static Boolean matchesListing(String query, ItemStack stack) {
        if (!available()) {
            return null;
        }
        try {
            Object hit = Class.forName(PLUGIN_CLASS)
                    .getMethod("matchesListing", String.class, ItemStack.class)
                    .invoke(null, query, stack);
            return hit instanceof Boolean value ? value : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
