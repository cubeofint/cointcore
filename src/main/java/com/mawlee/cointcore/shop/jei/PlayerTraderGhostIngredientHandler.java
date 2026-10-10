package com.mawlee.cointcore.shop.jei;

import com.mawlee.cointcore.shop.GhostTemplate;
import com.mawlee.cointcore.shop.client.PlayerTraderManageScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Lets JEI drag (ingredient list, bookmarks) drop a copy onto the manage phantom slot.
 */
public final class PlayerTraderGhostIngredientHandler implements IGhostIngredientHandler<PlayerTraderManageScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(
            PlayerTraderManageScreen gui,
            ITypedIngredient<I> ingredient,
            boolean doStart
    ) {
        ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (GhostTemplate.sanitize(stack).isEmpty()) {
            return List.of();
        }
        return List.of(new Target<>() {
            @Override
            public Rect2i getArea() {
                return gui.ghostDropArea();
            }

            @Override
            public void accept(I value) {
                gui.acceptGhostIngredient(stack);
            }
        });
    }

    @Override
    public void onComplete() {
    }
}
