package com.mawlee.cointcore.shop.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Vanilla-style cursor restore after a container screen is recreated.
 */
public final class GuiCursor {
    private static double savedX = Double.NaN;
    private static double savedY = Double.NaN;

    private GuiCursor() {
    }

    public static void remember() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.mouseHandler == null) {
            return;
        }
        savedX = minecraft.mouseHandler.xpos();
        savedY = minecraft.mouseHandler.ypos();
    }

    public static void restoreIfNeeded() {
        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            return;
        }
        restore();
    }

    public static void restore() {
        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getWindow() != null) {
            GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), savedX, savedY);
        }
        savedX = Double.NaN;
        savedY = Double.NaN;
    }
}
