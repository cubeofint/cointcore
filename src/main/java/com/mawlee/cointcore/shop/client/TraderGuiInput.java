package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.GuiEditBoxes;
import net.minecraft.client.gui.screens.Screen;

/**
 * Keeps inventory hotkeys (E, hotbar digits) from firing while a trader EditBox is focused.
 */
public final class TraderGuiInput {
    private TraderGuiInput() {
    }

    public static boolean handleKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        return GuiEditBoxes.handleKeyPressed(screen, keyCode, scanCode, modifiers);
    }

    public static boolean handleCharTyped(Screen screen, char codePoint, int modifiers) {
        return GuiEditBoxes.handleCharTyped(screen, codePoint, modifiers);
    }
}
