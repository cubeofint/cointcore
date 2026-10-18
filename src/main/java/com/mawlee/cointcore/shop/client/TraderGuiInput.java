package com.mawlee.cointcore.shop.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Keeps inventory hotkeys (E, hotbar digits) from firing while a trader {@link EditBox} is focused.
 */
public final class TraderGuiInput {
    private TraderGuiInput() {
    }

    public static boolean handleKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_TAB) {
            return false;
        }
        if (!(screen.getFocused() instanceof EditBox box) || !box.isFocused()) {
            return false;
        }
        box.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    public static boolean handleCharTyped(Screen screen, char codePoint, int modifiers) {
        if (!(screen.getFocused() instanceof EditBox box) || !box.canConsumeInput()) {
            return false;
        }
        return box.charTyped(codePoint, modifiers);
    }
}
