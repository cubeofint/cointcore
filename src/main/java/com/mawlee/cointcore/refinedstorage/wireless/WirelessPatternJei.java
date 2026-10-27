package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/** Native RS packets with a separate client class key, preserving the block Pattern Grid's JEI handlers. */
public final class WirelessPatternJei {
    private WirelessPatternJei() {
    }

    public static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new Handler<>(RecipeTypes.CRAFTING,
                (menu, slots) -> menu.transferCraftingRecipe(items(slots, RecipeIngredientRole.INPUT))), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new Handler<>(RecipeTypes.STONECUTTING, (menu, slots) -> {
            var inputs = items(slots, RecipeIngredientRole.INPUT);
            var outputs = items(slots, RecipeIngredientRole.OUTPUT);
            if (!inputs.isEmpty() && !inputs.getFirst().isEmpty() && !outputs.isEmpty() && !outputs.getFirst().isEmpty()) {
                menu.transferStonecutterRecipe(inputs.getFirst().getFirst(), outputs.getFirst().getFirst());
            }
        }), RecipeTypes.STONECUTTING);
        registration.addRecipeTransferHandler(new Handler<>(RecipeTypes.SMITHING, (menu, slots) -> {
            var inputs = items(slots, RecipeIngredientRole.INPUT);
            if (inputs.size() >= 3) menu.transferSmithingTableRecipe(inputs.get(0), inputs.get(1), inputs.get(2));
        }), RecipeTypes.SMITHING);
        registration.addUniversalRecipeTransferHandler(new ProcessingHandler());
    }

    private static List<List<ItemResource>> items(IRecipeSlotsView slots, RecipeIngredientRole role) {
        return slots.getSlotViews(role).stream().map(slot -> {
            var items = new ArrayList<>(slot.getItemStacks().filter(stack -> !stack.isEmpty())
                    .map(ItemResource::ofItemStack).toList());
            slot.getDisplayedItemStack().filter(stack -> !stack.isEmpty()).map(ItemResource::ofItemStack)
                    .ifPresent(displayed -> { items.remove(displayed); items.addFirst(displayed); });
            return (List<ItemResource>) items;
        }).toList();
    }

    private static List<List<ResourceAmount>> resources(IRecipeSlotsView slots, RecipeIngredientRole role) {
        var converter = RefinedStorageApi.INSTANCE.getIngredientConverter();
        return slots.getSlotViews(role).stream().map(slot -> {
            var resources = new ArrayList<>(slot.getAllIngredients()
                    .flatMap(ingredient -> converter.convertToResourceAmount(ingredient.getIngredient()).stream()).toList());
            slot.getDisplayedIngredient().flatMap(ingredient -> converter.convertToResourceAmount(ingredient.getIngredient()))
                    .ifPresent(displayed -> { resources.remove(displayed); resources.addFirst(displayed); });
            return (List<ResourceAmount>) resources;
        }).toList();
    }

    private record Handler<R>(RecipeType<R> recipeType, BiConsumer<PatternGridContainerMenu, IRecipeSlotsView> transfer)
            implements IRecipeTransferHandler<PatternGridContainerMenu, R> {
        @Override public Class<? extends PatternGridContainerMenu> getContainerClass() { return WirelessPatternContainerMenu.class; }
        @Override public Optional<MenuType<PatternGridContainerMenu>> getMenuType() { return Optional.of(WirelessPatternIntegration.MENU.get()); }
        @Override public RecipeType<R> getRecipeType() { return recipeType; }
        @Override public IRecipeTransferError transferRecipe(PatternGridContainerMenu menu, R recipe, IRecipeSlotsView slots,
                                                             Player player, boolean maxTransfer, boolean doTransfer) {
            if (doTransfer) transfer.accept(menu, slots);
            return null;
        }
    }

    private static final class ProcessingHandler implements IUniversalRecipeTransferHandler<PatternGridContainerMenu> {
        @Override public Class<? extends PatternGridContainerMenu> getContainerClass() { return WirelessPatternContainerMenu.class; }
        @Override public Optional<MenuType<PatternGridContainerMenu>> getMenuType() { return Optional.of(WirelessPatternIntegration.MENU.get()); }
        @Override public IRecipeTransferError transferRecipe(PatternGridContainerMenu menu, Object recipe, IRecipeSlotsView slots,
                                                             Player player, boolean maxTransfer, boolean doTransfer) {
            if (doTransfer) menu.transferProcessingRecipe(resources(slots, RecipeIngredientRole.INPUT), resources(slots, RecipeIngredientRole.OUTPUT));
            return null;
        }
    }
}
