package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.InvSeeScaleLayout;
import com.mawlee.cointcore.invsee.menu.InvSeeBackpackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeBackpackScreen extends InvSeeBaseScreen<InvSeeBackpackMenu> {
    private Button prevButton;
    private Button nextButton;

    public InvSeeBackpackScreen(InvSeeBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.viewerInventoryY() + 82;
    }

    @Override
    protected void initExtraWidgets() {
        var prevBox = InvSeeScaleLayout.sideButton(leftPos, imageWidth, width, topPos + 40, false);
        var nextBox = InvSeeScaleLayout.sideButton(leftPos, imageWidth, width, topPos + 40, true);
        prevButton = Button.builder(Component.literal("<"), b -> sendButton(InvSeeBackpackMenu.BUTTON_PREV_PAGE))
                .bounds(prevBox.x(), prevBox.y(), prevBox.w(), prevBox.h())
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.prev")))
                .build();
        nextButton = Button.builder(Component.literal(">"), b -> sendButton(InvSeeBackpackMenu.BUTTON_NEXT_PAGE))
                .bounds(nextBox.x(), nextBox.y(), nextBox.w(), nextBox.h())
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
