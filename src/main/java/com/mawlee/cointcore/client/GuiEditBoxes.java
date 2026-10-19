package com.mawlee.cointcore.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Vanilla-style {@link EditBox} lifecycle for container screens: create in
 * {@code init}, restore the saved string, never {@code setValue} from an
 * external sync while the user can type, and eat inventory hotkeys while focused.
 */
public final class GuiEditBoxes {
    private GuiEditBoxes() {
    }

    public static EditBox create(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component narration,
            String value,
            Consumer<String> responder
    ) {
        EditBox box = new EditBox(font, x, y, width, height, narration == null ? Component.empty() : narration);
        box.setValue(value == null ? "" : value);
        if (responder != null) {
            box.setResponder(text -> responder.accept(text == null ? "" : text));
        }
        return box;
    }

    public static void writeUnfocused(EditBox box, String text) {
        if (box == null) {
            return;
        }
        if (box.canConsumeInput()) {
            return;
        }
        write(box, text);
    }

    public static void write(EditBox box, String text) {
        if (box == null) {
            return;
        }
        String value = text == null ? "" : text;
        if (value.equals(box.getValue())) {
            return;
        }
        int cursor = box.getCursorPosition();
        box.setValue(value);
        int length = box.getValue().length();
        box.setCursorPosition(Math.min(cursor, length));
        box.setHighlightPos(Math.min(cursor, length));
    }

    public static void restoreFocus(Screen screen, EditBox box, boolean focused, int cursor) {
        if (screen == null || box == null || !focused) {
            return;
        }
        screen.setFocused(box);
        box.setFocused(true);
        int length = box.getValue().length();
        if (cursor >= 0) {
            int pos = Math.min(cursor, length);
            box.setCursorPosition(pos);
            box.setHighlightPos(pos);
        }
    }

    /**
     * While an {@link EditBox} is focused, keep inventory key E / hotbar digits
     * from closing the screen. Escape and Tab stay with the screen.
     */
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
