package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.shop.jei.CointJeiPlugin;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewScreen;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;
import java.util.List;

public final class PreviewJei implements IGuiContainerHandler<AutocraftingPreviewScreen> {
    public static void register(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(AutocraftingPreviewScreen.class, new PreviewJei());
    }

    public static boolean openRecipe(AutocraftingPreviewScreen screen, double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;
        var runtime = CointJeiPlugin.runtime();
        if (runtime == null) return false;
        var ingredient = new PreviewJei().getClickableIngredientUnderMouse(screen, mouseX, mouseY);
        if (ingredient.isEmpty()) return false;
        show(runtime, ingredient.get(), button == 0 ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT);
        return true;
    }

    private static <T> void show(IJeiRuntime runtime, IClickableIngredient<T> ingredient, RecipeIngredientRole role) {
        var focus = runtime.getJeiHelpers().getFocusFactory()
                .createFocus(role, ingredient.getIngredientType(), ingredient.getIngredient());
        runtime.getRecipesGui().show(List.of(focus));
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(AutocraftingPreviewScreen screen,
                                                                           double mouseX, double mouseY) {
        var runtime = CointJeiPlugin.runtime();
        if (runtime == null || !(screen instanceof PreviewIngredients ingredients)) return Optional.empty();
        for (var hit : ingredients.cointcore$previewIngredients()) {
            var area = hit.area();
            if (mouseX < area.getX() || mouseX >= area.getX() + area.getWidth()
                    || mouseY < area.getY() || mouseY >= area.getY() + area.getHeight()) continue;
            Object ingredient;
            if (hit.resource() instanceof ItemResource item) {
                ingredient = item.toItemStack(1);
            } else if (hit.resource() instanceof FluidResource fluid) {
                ingredient = new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid.fluid()), 1000, fluid.components());
            } else {
                return Optional.empty();
            }
            return runtime.getIngredientManager().createClickableIngredient(ingredient, area, true)
                    .map(value -> value);
        }
        return Optional.empty();
    }
}
