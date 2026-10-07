package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.InvSeeTab;
import com.mawlee.cointcore.invsee.menu.InvSeeInfoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeInfoScreen extends InvSeeBaseScreen<InvSeeInfoMenu> {
    public InvSeeInfoScreen(InvSeeInfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = InvSeeInfoMenu.VIEWER_INVENTORY_Y + 82;
    }

    @Override
    protected boolean showEditToggle() {
        return false;
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        InvSeeTab tab = InvSeeTab.fromOrdinalOrInventory(InvSeeClientChrome.activeTab());
        if (tab != InvSeeTab.FTB && tab != InvSeeTab.GRAVES && tab != InvSeeTab.STATE) {
            return;
        }
        String expected = tab.section().id();
        if (!expected.equals(InvSeeClientChrome.infoKind())) {
            return;
        }
        int y = 18;
        int maxY = menu.viewerInventoryY() - 16;
        for (String line : InvSeeClientChrome.infoLines()) {
            String clipped = font.plainSubstrByWidth(line, imageWidth - 16);
            graphics.drawString(font, clipped, 8, y, VanillaContainerSkin.LABEL_COLOR, false);
            y += 10;
            if (y > maxY) {
                break;
            }
        }
    }
}
