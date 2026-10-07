package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeInfoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeInfoScreen extends InvSeeBaseScreen<InvSeeInfoMenu> {
    public InvSeeInfoScreen(InvSeeInfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = 88;
    }

    @Override
    protected boolean showEditToggle() {
        return false;
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = 8;
        for (String line : InvSeeClientChrome.infoLines()) {
            String clipped = font.plainSubstrByWidth(line, imageWidth - 16);
            graphics.drawString(font, clipped, 8, y, InvSeeTheme.TEXT, false);
            y += 10;
            if (y > 80) {
                break;
            }
        }
    }
}
