package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.refinedstorage.batch.BatchRecipeTarget;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.grid.AbstractCraftingGridContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.refinedmods.refinedstorage.jei.common.CraftingGridRecipeTransferHandler", remap = false)
public abstract class BatchJeiRecipeTargetMixin {
    @WrapMethod(method = "transferRecipe(Lcom/refinedmods/refinedstorage/common/grid/AbstractCraftingGridContainerMenu;Lnet/minecraft/world/item/crafting/RecipeHolder;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;")
    private IRecipeTransferError cointcore$recipeTarget(AbstractCraftingGridContainerMenu menu,
            RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player,
            boolean maxTransfer, boolean doTransfer, Operation<IRecipeTransferError> original) {
        if (!doTransfer) return original.call(menu, recipe, slots, player, maxTransfer, false);
        ItemStack output = slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .flatMap(slot -> slot.getItemStacks()).filter(stack -> !stack.isEmpty()).findFirst()
                .orElseGet(() -> recipe.value().getResultItem(player.registryAccess()));
        var previous = BatchRecipeTarget.replace(output.isEmpty() ? null
                : new ResourceAmount(ItemResource.ofItemStack(output), output.getCount()));
        try {
            return original.call(menu, recipe, slots, player, maxTransfer, true);
        } finally {
            BatchRecipeTarget.replace(previous);
        }
    }
}
