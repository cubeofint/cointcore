package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.CointCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class InventoryMarketButton extends Button {
    public static final int SIZE = 16;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "textures/gui/market_button.png");

    public InventoryMarketButton(int x, int y, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("gui.cointcore.market.button")));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float v = isHoveredOrFocused() ? SIZE : 0;
        graphics.blit(TEXTURE, getX(), getY(), SIZE, SIZE, 0.0f, v, SIZE, SIZE, SIZE, SIZE * 2);
    }
}
