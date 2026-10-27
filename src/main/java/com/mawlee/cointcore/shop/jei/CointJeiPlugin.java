package com.mawlee.cointcore.shop.jei;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.client.PlayerTraderManageScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class CointJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        if (net.neoforged.fml.ModList.get().isLoaded("refinedstorage")
                && net.neoforged.fml.ModList.get().isLoaded("universalgrid")) {
            com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternJei.register(registration);
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
                PlayerTraderManageScreen.class,
                new PlayerTraderGhostIngredientHandler()
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }

    public static boolean matchesListing(String query, ItemStack listing) {
        List<ItemStack> filtered = filteredStacks(query);
        if (filtered == null || listing == null || listing.isEmpty()) {
            return false;
        }
        for (ItemStack ingredient : filtered) {
            if (ingredient != null && !ingredient.isEmpty() && ItemStack.isSameItemSameComponents(listing, ingredient)) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> filteredStacks(String query) {
        IIngredientFilter filter = filter();
        if (filter == null) {
            return null;
        }
        String text = query == null ? "" : query;
        if (!text.equals(filter.getFilterText())) {
            filter.setFilterText(text);
        }
        List<ItemStack> ingredients = filter.getFilteredIngredients(VanillaTypes.ITEM_STACK);
        if (ingredients == null) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(ingredients.size());
        for (ItemStack stack : ingredients) {
            if (stack != null && !stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static IIngredientFilter filter() {
        return runtime == null ? null : runtime.getIngredientFilter();
    }
}
