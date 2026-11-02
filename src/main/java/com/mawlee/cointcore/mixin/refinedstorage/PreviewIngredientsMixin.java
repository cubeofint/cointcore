package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.PreviewIngredients;
import com.mawlee.cointcore.refinedstorage.PreviewJei;
import com.refinedmods.refinedstorage.api.autocrafting.preview.PreviewItem;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AutocraftingPreviewScreen.class, remap = false)
public abstract class PreviewIngredientsMixin implements PreviewIngredients {
    @Unique private final List<Hit> cointcore$ingredients = new ArrayList<>();

    @Inject(method = "init", at = @At("TAIL"))
    private void cointcore$allowJeiFocus(CallbackInfo ci) {
        var screen = (AutocraftingPreviewScreen) (Object) this;
        var field = ((PreviewAmountFieldAccessor) screen).cointcore$amountField();
        if (field != null) {
            field.setCanLoseFocus(true);
            field.setFocused(false);
            screen.setFocused(null);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cointcore$openJeiRecipe(double mouseX, double mouseY, int button,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (ModList.get().isLoaded("jei")
                && PreviewJei.openRecipe((AutocraftingPreviewScreen) (Object) this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "renderBg", at = @At("HEAD"))
    private void cointcore$clearIngredients(GuiGraphics graphics, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        cointcore$ingredients.clear();
    }

    @Inject(method = "renderCell", at = @At("HEAD"))
    private void cointcore$trackIngredient(GuiGraphics graphics, int x, int y, PreviewItem item,
                                          double mouseX, double mouseY, CallbackInfo ci) {
        var screen = (AutocraftingPreviewScreen) (Object) this;
        // Match the native scissor, including partially visible rows during smooth scrolling.
        int left = Math.max(x, screen.getGuiLeft() + 8);
        int right = Math.min(x + 73, screen.getGuiLeft() + 8 + 221);
        int top = Math.max(y, screen.getGuiTop() + 98);
        int bottom = Math.min(y + 29, screen.getGuiTop() + 98 + 119);
        if (right > left && bottom > top) {
            cointcore$ingredients.add(new Hit(item.resource(), new Rect2i(left, top, right - left, bottom - top)));
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                // RS otherwise consumes every key while its quantity field has focus, even R/U/A.
                var field = ((PreviewAmountFieldAccessor) screen).cointcore$amountField();
                if (field != null && field.isFocused()) {
                    field.setFocused(false);
                    screen.setFocused(null);
                }
            }
        }
    }

    @Override
    public List<Hit> cointcore$previewIngredients() {
        return cointcore$ingredients;
    }
}
