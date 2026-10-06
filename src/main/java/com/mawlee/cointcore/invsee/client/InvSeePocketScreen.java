package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeePocketMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeePocketScreen extends InvSeeBaseScreen<InvSeePocketMenu> {
    private InvSeeFlatButton prevButton;
    private InvSeeFlatButton nextButton;

    public InvSeePocketScreen(InvSeePocketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + InvSeePocketMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void initExtraWidgets() {
        prevButton = new InvSeeFlatButton(
                leftPos - 22,
                topPos + 40,
                18,
                16,
                Component.literal("<"),
                b -> sendButton(InvSeePocketMenu.BUTTON_PREV_PAGE)
        );
        prevButton.setTooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.prev")));
        nextButton = new InvSeeFlatButton(
                leftPos + imageWidth + 4,
                topPos + 40,
                18,
                16,
                Component.literal(">"),
                b -> sendButton(InvSeePocketMenu.BUTTON_NEXT_PAGE)
        );
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
