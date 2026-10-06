package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeCosmeticMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeCosmeticScreen extends InvSeeBaseScreen<InvSeeCosmeticMenu> {
    public InvSeeCosmeticScreen(InvSeeCosmeticMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        int rows = InvSeeCosmeticMenu.ROWS;
        this.imageWidth = 176;
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.cosmetic"),
                8,
                6,
                InvSeeTheme.FAINT,
                false
        );
    }
}
