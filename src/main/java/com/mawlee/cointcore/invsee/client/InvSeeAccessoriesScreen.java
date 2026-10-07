package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.menu.InvSeeAccessoriesMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeAccessoriesScreen extends InvSeeBaseScreen<InvSeeAccessoriesMenu> {
    private Button prevButton;
    private Button nextButton;

    public InvSeeAccessoriesScreen(InvSeeAccessoriesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.viewerInventoryY() + 82;
    }

    @Override
    protected void initExtraWidgets() {
        int midY = topPos + 18 + (InvSeeAccessoriesMenu.ROWS * 18) / 2 - 8;
        prevButton = Button.builder(Component.literal("<"), b -> sendButton(InvSeeAccessoriesMenu.BUTTON_PREV_PAGE))
                .bounds(leftPos - 22, midY, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.prev")))
                .build();
        nextButton = Button.builder(Component.literal(">"), b -> sendButton(InvSeeAccessoriesMenu.BUTTON_NEXT_PAGE))
                .bounds(leftPos + imageWidth + 4, midY, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.next")))
                .build();
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
            graphics.drawString(font, page, imageWidth - 8 - font.width(page), 6, VanillaContainerSkin.LABEL_COLOR, false);
        }
    }
}
