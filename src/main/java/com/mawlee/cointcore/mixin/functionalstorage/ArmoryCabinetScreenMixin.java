package com.mawlee.cointcore.mixin.functionalstorage;

import com.mawlee.cointcore.functionalstorage.ArmoryCabinetQueryAccess;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Functional Storage's Armory Cabinet search box is not focused in {@code init}
 * and is recreated empty on resize. JEI then eats keystrokes, and {@code init}
 * would send {@code BUTTON_QUERY_CLEAR}. Focus and restore like creative search.
 */
@Pseudo
@Mixin(targets = "com.buuz135.functionalstorage.client.gui.ArmoryCabinetScreen", remap = false)
public abstract class ArmoryCabinetScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Shadow
    private EditBox searchBox;

    protected ArmoryCabinetScreenMixin(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init()V", at = @At("RETURN"), remap = false)
    private void cointcore$restoreAndFocusSearch(CallbackInfo ci) {
        if (searchBox == null) {
            return;
        }
        String query = menu instanceof ArmoryCabinetQueryAccess access ? access.cointcore$query() : "";
        if (query == null) {
            query = "";
        }
        if (!query.equals(searchBox.getValue())) {
            searchBox.setValue(query);
        }
        searchBox.setCanLoseFocus(false);
        setInitialFocus(searchBox);
        searchBox.setFocused(true);
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), remap = false, cancellable = true)
    private void cointcore$keyPressedSearch(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return;
        }
        if (searchBox != null && searchBox.isFocused()) {
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            cir.setReturnValue(true);
        }
    }
}
