package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeAttachmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeAttachmentScreen extends InvSeeBaseScreen<InvSeeAttachmentMenu> {
    public InvSeeAttachmentScreen(InvSeeAttachmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + InvSeeAttachmentMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected boolean showEditToggle() {
        return false;
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.readonly"),
                8,
                6,
                InvSeeTheme.MUTED,
                false
        );
    }
}
