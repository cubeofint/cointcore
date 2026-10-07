package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeCuriosScreen extends InvSeeBaseScreen<InvSeeCuriosMenu> {
    private InvSeeFlatButton prevButton;
    private InvSeeFlatButton nextButton;

    public InvSeeCuriosScreen(InvSeeCuriosMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.viewerInventoryY() + 82;
    }

    @Override
    protected void initExtraWidgets() {
        int midY = topPos + 18 + (InvSeeCuriosMenu.ROWS * 18) / 2 - 8;
        prevButton = new InvSeeFlatButton(
                leftPos - 22,
                midY,
                18,
                16,
                Component.literal("<"),
                b -> sendButton(InvSeeCuriosMenu.BUTTON_PREV_PAGE)
        ).style(InvSeeFlatButton.Style.NEUTRAL);
        prevButton.setTooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.prev")));
        nextButton = new InvSeeFlatButton(
                leftPos + imageWidth + 4,
                midY,
                18,
                16,
                Component.literal(">"),
                b -> sendButton(InvSeeCuriosMenu.BUTTON_NEXT_PAGE)
        ).style(InvSeeFlatButton.Style.NEUTRAL);
        nextButton.setTooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.next")));
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
        updatePageButtons();
    }

    private void updatePageButtons() {
        boolean multi = menu.maxPage() > 0;
        prevButton.visible = multi;
        nextButton.visible = multi;
        prevButton.active = menu.page() > 0;
        nextButton.active = menu.page() < menu.maxPage();
    }

    @Override
    protected void tickExtraWidgets() {
        updatePageButtons();
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.maxPage() > 0) {
            Component page = Component.translatable(
                    "gui.cointcore.invsee.page.status",
                    menu.page() + 1,
                    menu.maxPage() + 1
            );
            graphics.drawString(font, page, 8, 6, InvSeeTheme.MUTED, false);
        }
    }
}
