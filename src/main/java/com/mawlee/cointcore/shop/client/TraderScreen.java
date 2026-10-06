package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.TraderMenu;
import com.mawlee.cointcore.shop.TraderOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TraderScreen extends AbstractContainerScreen<TraderMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public TraderScreen(TraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + TraderMenu.CONTAINER_ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component balance = Component.translatable("container.cointcore.trader.balance", menu.gluonBalance());
        graphics.drawString(font, balance, imageWidth - 8 - font.width(balance), 6, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.index < menu.offers().size()) {
            TraderOffer offer = menu.offers().get(hoveredSlot.index);
            List<Component> lines = new ArrayList<>();
            ItemStack stack = offer.display();
            lines.add(stack.getHoverName());
            lines.add(Component.translatable("container.cointcore.trader.hint"));
            if (offer.canBuy()) {
                lines.add(Component.translatable(
                        "container.cointcore.trader.tooltip.buy",
                        offer.buyTotal(),
                        offer.buyPrice(),
                        offer.buyFee()
                ));
            }
            if (offer.canSell()) {
                lines.add(Component.translatable(
                        "container.cointcore.trader.tooltip.sell",
                        offer.sellNet(),
                        offer.sellPrice(),
                        offer.sellFee()
                ));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, TraderMenu.CONTAINER_ROWS * 18 + 17);
        graphics.blit(BACKGROUND, x, y + TraderMenu.CONTAINER_ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }
}
