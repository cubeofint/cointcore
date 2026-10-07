package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.InvSeeCursorGuard;
import com.mawlee.cointcore.mixin.client.accessor.MouseHandlerAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Vanilla {@code openMenu} closes the current screen first ({@code setScreen(null)}),
 * which grabs the mouse and recenters it. Restore the previous cursor after the
 * replacement InvSee screen opens.
 */
public final class InvSeeCursorPreserve {
    private static double savedX;
    private static double savedY;
    private static boolean saved;
    private static int savedTick;

    private InvSeeCursorPreserve() {
    }

    public static void captureIfInvSee(Screen current, Screen next) {
        if (!(current instanceof InvSeeBaseScreen<?>)) {
            return;
        }
        if (next != null && !(next instanceof InvSeeBaseScreen<?>)) {
            saved = false;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouse = minecraft.mouseHandler;
        savedX = mouse.xpos();
        savedY = mouse.ypos();
        saved = true;
        savedTick = guiTick(minecraft);
    }

    public static void restoreIfInvSee(Screen opened) {
        if (!(opened instanceof InvSeeBaseScreen<?> screen)) {
            if (opened != null) {
                saved = false;
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!InvSeeCursorGuard.shouldRestore(saved, savedTick, guiTick(minecraft))) {
            saved = false;
            return;
        }
        saved = false;
        apply(minecraft, screen, savedX, savedY);
    }

    private static void apply(Minecraft minecraft, InvSeeBaseScreen<?> screen, double x, double y) {
        MouseHandler mouse = minecraft.mouseHandler;
        ((MouseHandlerAccessor) mouse).cointcore$setXpos(x);
        ((MouseHandlerAccessor) mouse).cointcore$setYpos(y);
        ((MouseHandlerAccessor) mouse).cointcore$setIgnoreFirstMove(false);
        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x, y);

        int screenWidth = minecraft.getWindow().getScreenWidth();
        int screenHeight = minecraft.getWindow().getScreenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            screen.resetHoverAfterTabSwitch(0, 0);
            return;
        }
        double guiX = x * minecraft.getWindow().getGuiScaledWidth() / screenWidth;
        double guiY = y * minecraft.getWindow().getGuiScaledHeight() / screenHeight;
        screen.resetHoverAfterTabSwitch(guiX, guiY);
    }

    private static int guiTick(Minecraft minecraft) {
        return minecraft.gui.getGuiTicks();
    }
}
