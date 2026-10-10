package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.shop.OfferPriceStats;
import com.mawlee.cointcore.shop.OfferPriceVerdict;
import com.mawlee.cointcore.shop.TraderFeedbackLines;
import com.mawlee.cointcore.shop.TraderFeedbackPayload;
import com.mawlee.cointcore.shop.TraderMenu;
import com.mawlee.cointcore.shop.TraderOffer;
import com.mawlee.cointcore.shop.TraderShopLayout;
import com.mawlee.cointcore.shop.TraderTradePayload;
import com.mawlee.cointcore.ui.ScaledGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class TraderScreen extends AbstractContainerScreen<TraderMenu> {
    private static final int TEXT_LEFT = 28;
    private static final int ICON_LEFT = 8;
    private static final int ICON_SIZE = 18;
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 2;
    private static final int NAME_OFFSET = 2;
    private static final int PRICE_BLOCK_TOP = 14;
    private static final int CHART_GAP = 4;
    private static final String ELLIPSIS = "…";

    private TraderShopLayout layout = TraderShopLayout.preferredTerminal();
    private int page;
    private int buttonWidth;
    private int buttonColumnX;
    private int textMaxWidth;
    private TraderFeedbackPayload feedback;

    public TraderScreen(TraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TraderMenu.GUI_WIDTH;
        this.imageHeight = TraderMenu.GUI_HEIGHT;
        this.inventoryLabelX = TraderMenu.PLAYER_INV_LEFT;
        this.inventoryLabelY = TraderMenu.PLAYER_INV_Y - 12;
    }

    @Override
    protected void init() {
        layout = TraderShopLayout.terminal(width, height);
        imageWidth = layout.guiWidth();
        imageHeight = layout.guiHeight();
        inventoryLabelX = layout.playerInvLeft();
        inventoryLabelY = layout.playerInvY() - 12;
        GuiSlotMover.movePlayerInventory(menu.slots, 0, layout.playerInvLeft(), layout.playerInvY());
        super.init();
        leftPos = ScaledGuiLayout.origin(width, imageWidth, 0, 0, ScaledGuiLayout.MARGIN);
        topPos = ScaledGuiLayout.origin(height, imageHeight, 0, 0, ScaledGuiLayout.MARGIN);
        page = Math.min(page, maxPage());
        buttonWidth = measureButtonWidth();
        buttonColumnX = imageWidth - 8 - buttonWidth;
        int chartReserve = OfferPriceSparkline.WIDTH + CHART_GAP + GluonGuiIcon.SIZE + 2;
        textMaxWidth = Math.max(16, buttonColumnX - chartReserve - TEXT_LEFT);
        int listLeft = leftPos + 8;
        int buttonY0 = topPos + layout.listTop();
        int start = page * layout.pageSize();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            TraderOffer offer = menu.offers().get(offerIndex);
            int y = buttonY0 + row * layout.rowHeight() + PRICE_BLOCK_TOP;
            int buyX = leftPos + buttonColumnX;
            Button buy = Button.builder(Component.translatable("gui.cointcore.trader.buy"), button -> trade(offerIndex, false))
                    .bounds(buyX, y, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.trader.shift_hint")))
                    .build();
            buy.active = offer.canBuy();
            addRenderableWidget(buy);
            Button sell = Button.builder(Component.translatable("gui.cointcore.trader.sell"), button -> trade(offerIndex, true))
                    .bounds(buyX, y + BUTTON_HEIGHT + BUTTON_GAP, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.trader.shift_hint")))
                    .build();
            sell.active = offer.canSell();
            addRenderableWidget(sell);
        }

        int pagerY = topPos + layout.pagerY();
        Button prev = Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(listLeft, pagerY, 16, 16)
                .build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(leftPos + imageWidth - 24, pagerY, 16, 16)
                .build();
        next.active = page < maxPage();
        addRenderableWidget(next);
    }

    void setFeedback(TraderFeedbackPayload payload) {
        this.feedback = payload;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOfferItems(graphics);
        renderSparklines(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, layout.titleY(), VanillaContainerSkin.LABEL_COLOR, false);
        Component balance = Component.translatable("container.cointcore.trader.balance", menu.gluonBalance());
        graphics.drawString(font, balance, titleLabelX, layout.balanceY(), VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, titleLabelX + font.width(balance) + 2, layout.balanceY() - 1);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);

        int start = page * layout.pageSize();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            TraderOffer offer = menu.offers().get(offerIndex);
            int y = layout.rowY(row);
            ItemStack stack = offer.display();
            graphics.drawString(
                    font,
                    ellipsize(stack.getHoverName().getString(), textMaxWidth),
                    TEXT_LEFT,
                    y + NAME_OFFSET,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            int buyY = y + PRICE_BLOCK_TOP + priceTextOffset();
            int sellY = y + PRICE_BLOCK_TOP + BUTTON_HEIGHT + BUTTON_GAP + priceTextOffset();
            drawPriceLine(
                    graphics,
                    Component.translatable(
                            "gui.cointcore.trader.buy_price",
                            offer.canBuy() ? Long.toString(offer.buyTotal()) : "—"
                    ),
                    buyY
            );
            drawPriceLine(
                    graphics,
                    Component.translatable(
                            "gui.cointcore.trader.sell_price",
                            offer.canSell() ? Long.toString(offer.sellNet()) : "—"
                    ),
                    sellY
            );
        }

        if (menu.offers().isEmpty()) {
            Component empty = Component.translatable("gui.cointcore.trader.empty");
            graphics.drawString(
                    font,
                    empty,
                    (imageWidth - font.width(empty)) / 2,
                    layout.titleHeight() + 20,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
        }

        int pages = maxPage() + 1;
        Component pageLabel = Component.translatable("gui.cointcore.trader.page", page + 1, pages);
        int pagerY = layout.offerPanelHeight() - 13;
        graphics.drawString(
                font,
                pageLabel,
                (imageWidth - font.width(pageLabel)) / 2,
                pagerY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );

        if (feedback != null) {
            Component line = feedbackLine(feedback);
            int color = feedback.kind().error() ? 0xAA0000 : 0x2E7D32;
            int statusY = layout.statusY();
            graphics.drawString(
                    font,
                    Component.literal(font.plainSubstrByWidth(line.getString(), imageWidth - 16)),
                    8,
                    statusY,
                    color,
                    false
            );
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int chartOffer = hoveredChartOffer(mouseX, mouseY);
        if (chartOffer >= 0) {
            graphics.renderComponentTooltip(font, historyTooltip(menu.offers().get(chartOffer)), mouseX, mouseY);
            return;
        }
        int offerIndex = hoveredOffer(mouseX, mouseY);
        if (offerIndex >= 0) {
            TraderOffer offer = menu.offers().get(offerIndex);
            if (hoveredIcon(mouseX, mouseY, offerIndex)) {
                graphics.renderTooltip(font, offer.display(), mouseX, mouseY);
                return;
            }
            if (hoveredName(mouseX, mouseY, offerIndex)) {
                graphics.renderTooltip(font, offer.display().getHoverName(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        VanillaContainerSkin.blitPanel(graphics, x, y, imageWidth, imageHeight);
        int start = page * layout.pageSize();
        int rowLeft = x + 7;
        int rowWidth = imageWidth - 14;
        for (int row = 0; row < layout.pageSize() && start + row < menu.offers().size(); row++) {
            int rowY = y + layout.rowY(row);
            VanillaContainerSkin.blitOfferRow(graphics, rowLeft, rowY, rowWidth, layout.rowHeight() - 2);
            VanillaContainerSkin.blitSlot(graphics, x + 7, rowY);
        }
        VanillaContainerSkin.blitMenuSlots(graphics, x, y, menu.slots);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth
                && mouseY >= topPos + layout.titleHeight()
                && mouseY < topPos + layout.offerPanelHeight()) {
            if (scrollY > 0) {
                changePage(-1);
                return true;
            }
            if (scrollY < 0) {
                changePage(1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderOfferItems(GuiGraphics graphics) {
        int start = page * layout.pageSize();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            ItemStack stack = menu.offers().get(offerIndex).display();
            int itemX = leftPos + ICON_LEFT;
            int itemY = topPos + layout.rowY(row) + 1;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private void renderSparklines(GuiGraphics graphics) {
        int start = page * layout.pageSize();
        double band = TraderOffersConfig.priceHistoryAverageBandPercent();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            TraderOffer offer = menu.offers().get(offerIndex);
            OfferPriceStats stats = OfferPriceStats.of(offer.buyHistory(), offer.buyPrice());
            OfferPriceVerdict verdict = OfferPriceVerdict.classify(stats.current(), stats.average(), band);
            OfferPriceSparkline.blit(graphics, chartScreenX(), chartScreenY(row), offer.buyHistory(), verdict);
        }
    }

    private List<Component> historyTooltip(TraderOffer offer) {
        OfferPriceStats stats = OfferPriceStats.of(offer.buyHistory(), offer.buyPrice());
        if (stats.count() == 0) {
            return List.of(Component.translatable("gui.cointcore.trader.history.empty"));
        }
        OfferPriceVerdict verdict = OfferPriceVerdict.classify(
                stats.current(),
                stats.average(),
                TraderOffersConfig.priceHistoryAverageBandPercent()
        );
        int gap = OfferPriceVerdict.percentGap(stats.current(), stats.average());
        Component verdictLine = switch (verdict) {
            case CHEAP -> Component.translatable("gui.cointcore.trader.history.cheap", gap);
            case EXPENSIVE -> Component.translatable("gui.cointcore.trader.history.expensive", gap);
            case FAIR -> Component.translatable("gui.cointcore.trader.history.fair");
            case UNKNOWN -> Component.translatable("gui.cointcore.trader.history.empty");
        };
        return List.of(
                Component.translatable("gui.cointcore.trader.history.min", stats.min()),
                Component.translatable("gui.cointcore.trader.history.avg", stats.average()),
                Component.translatable("gui.cointcore.trader.history.max", stats.max()),
                Component.translatable("gui.cointcore.trader.history.current", stats.current()),
                verdictLine
        );
    }

    private int chartScreenX() {
        return leftPos + buttonColumnX - CHART_GAP - OfferPriceSparkline.WIDTH;
    }

    private int chartScreenY(int row) {
        int blockTop = topPos + layout.rowY(row) + PRICE_BLOCK_TOP;
        int blockHeight = BUTTON_HEIGHT * 2 + BUTTON_GAP;
        return blockTop + (blockHeight - OfferPriceSparkline.HEIGHT) / 2;
    }

    private void drawPriceLine(GuiGraphics graphics, Component line, int y) {
        graphics.drawString(font, line, TEXT_LEFT, y, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, TEXT_LEFT + font.width(line) + 2, y - 1);
    }

    private int priceTextOffset() {
        return Math.max(0, (BUTTON_HEIGHT - 8) / 2);
    }

    private int hoveredChartOffer(int mouseX, int mouseY) {
        int start = page * layout.pageSize();
        int chartX = chartScreenX();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            int chartY = chartScreenY(row);
            if (mouseX >= chartX && mouseX < chartX + OfferPriceSparkline.WIDTH
                    && mouseY >= chartY && mouseY < chartY + OfferPriceSparkline.HEIGHT) {
                return offerIndex;
            }
        }
        return -1;
    }

    private void trade(int offerIndex, boolean sell) {
        PacketDistributor.sendToServer(new TraderTradePayload(menu.containerId, offerIndex, sell, hasShiftDown()));
    }

    private void changePage(int delta) {
        int next = page + delta;
        int clamped = Math.max(0, Math.min(maxPage(), next));
        if (clamped != page) {
            page = clamped;
            rebuildWidgets();
        }
    }

    private int maxPage() {
        int size = menu.offers().size();
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / layout.pageSize();
    }

    private int measureButtonWidth() {
        int buy = font.width(Component.translatable("gui.cointcore.trader.buy")) + BUTTON_PADDING;
        int sell = font.width(Component.translatable("gui.cointcore.trader.sell")) + BUTTON_PADDING;
        return Math.max(buy, sell);
    }

    private Component ellipsize(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return Component.literal(text);
        }
        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return Component.literal(ELLIPSIS);
        }
        return Component.literal(font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS);
    }

    private int hoveredOffer(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - layout.titleHeight();
        if (localY < 2) {
            return -1;
        }
        int row = (localY - 2) / layout.rowHeight();
        if (row < 0 || row >= layout.pageSize()) {
            return -1;
        }
        int rowTop = 2 + row * layout.rowHeight();
        if (localY < rowTop || localY >= rowTop + ICON_SIZE) {
            return -1;
        }
        if (localX < ICON_LEFT - 1 || localX >= TEXT_LEFT + textMaxWidth) {
            return -1;
        }
        int offerIndex = page * layout.pageSize() + row;
        if (offerIndex >= menu.offers().size()) {
            return -1;
        }
        return offerIndex;
    }

    private boolean hoveredIcon(int mouseX, int mouseY, int offerIndex) {
        int row = offerIndex - page * layout.pageSize();
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - layout.titleHeight();
        int rowTop = 2 + row * layout.rowHeight();
        return localX >= ICON_LEFT - 1 && localX < ICON_LEFT + ICON_SIZE
                && localY >= rowTop && localY < rowTop + ICON_SIZE;
    }

    private boolean hoveredName(int mouseX, int mouseY, int offerIndex) {
        int row = offerIndex - page * layout.pageSize();
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - layout.titleHeight();
        int rowTop = 2 + row * layout.rowHeight() + NAME_OFFSET;
        return localX >= TEXT_LEFT && localX < TEXT_LEFT + textMaxWidth
                && localY >= rowTop && localY < rowTop + 10;
    }

    private Component feedbackLine(TraderFeedbackPayload payload) {
        return TraderFeedbackLines.of(payload);
    }
}
