package com.mawlee.cointcore.invsee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Flat copper-toned button for InvSee chrome (no vanilla widget texture).
 */
public final class InvSeeFlatButton extends Button {
    private Style style = Style.NEUTRAL;

    public enum Style {
        NEUTRAL,
        ACCENT,
        EDIT
    }

    public InvSeeFlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public InvSeeFlatButton style(Style style) {
        this.style = style;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bg;
        int border;
        int fg;
        if (!active) {
            bg = InvSeeTheme.BTN_DISABLED;
            border = InvSeeTheme.LINE_SOFT;
            fg = InvSeeTheme.FAINT;
        } else if (style == Style.EDIT) {
            bg = isHoveredOrFocused() ? 0x3AC47A6A : InvSeeTheme.EDIT_SOFT;
            border = 0xFFC47A6A;
            fg = InvSeeTheme.EDIT;
        } else if (style == Style.ACCENT) {
            bg = isHoveredOrFocused() ? 0x3CC8894A : InvSeeTheme.ACCENT_SOFT;
            border = 0xFFC8894A;
            fg = InvSeeTheme.ACCENT_TEXT;
        } else {
            bg = isHoveredOrFocused() ? InvSeeTheme.BTN_HOVER : InvSeeTheme.BTN;
            border = InvSeeTheme.LINE;
            fg = InvSeeTheme.TEXT;
        }

        InvSeeUi.fill(graphics, getX(), getY(), width, height, bg);
        InvSeeUi.fill(graphics, getX(), getY(), width, 1, border);
        InvSeeUi.fill(graphics, getX(), getY() + height - 1, width, 1, border);
        InvSeeUi.fill(graphics, getX(), getY(), 1, height, border);
        InvSeeUi.fill(graphics, getX() + width - 1, getY(), 1, height, border);

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                fg
        );
    }
}
