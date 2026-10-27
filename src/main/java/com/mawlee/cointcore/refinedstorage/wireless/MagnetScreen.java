package com.mawlee.cointcore.refinedstorage.wireless;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MagnetScreen extends AbstractContainerScreen<MagnetMenu> {
    private Button modeButton;

    public MagnetScreen(MagnetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 176;
        inventoryLabelY = 82;
    }

    @Override protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(Component.translatable(menu.mode().translationKey()), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 8, topPos + 51, 160, 20).build());
        updateButton();
    }

    private void updateButton() {
        modeButton.setMessage(Component.translatable(menu.mode().translationKey()));
        modeButton.active = menu.hasCard();
    }

    @Override protected void containerTick() { super.containerTick(); updateButton(); }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int x, int y) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff373737);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xffc6c6c6);
        for (var slot : menu.slots) {
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xff373737);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xffffffff);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
