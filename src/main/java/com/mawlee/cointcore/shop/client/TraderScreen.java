package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.shop.OfferPriceStats;
import com.mawlee.cointcore.shop.OfferPriceVerdict;
import com.mawlee.cointcore.shop.TraderFeedbackKind;
import com.mawlee.cointcore.shop.TraderFeedbackPayload;
import com.mawlee.cointcore.shop.TraderMenu;
import com.mawlee.cointcore.shop.TraderOffer;
import com.mawlee.cointcore.shop.TraderTradePayload;
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
    private static final int CHART_GAP = 4;
    private static final String ELLIPSIS = "…";

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
        super.init();
        page = Math.min(page, maxPage());
        buttonWidth = measureButtonWidth();
        buttonColumnX = imageWidth - 8 - buttonWidth;
        int chartReserve = OfferPriceSparkline.WIDTH + CHART_GAP + GluonGuiIcon.SIZE + 2;
        textMaxWidth = Math.max(16, buttonColumnX - chartReserve - TEXT_LEFT);
        int listLeft = leftPos + 8;
        int buttonY0 = topPos + TraderMenu.TITLE_HEIGHT + 2;
        int start = page * TraderMenu.PAGE_SIZE;
        for (int row = 0; row < TraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            TraderOffer offer = menu.offers().get(offerIndex);
            int y = buttonY0 + row * TraderMenu.ROW_HEIGHT;
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

        int pagerY = topPos + TraderMenu.OFFER_PANEL_HEIGHT - TraderMenu.PAGE_BAR_HEIGHT + 1;
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
        graphics.drawString(font, title, titleLabelX, 6, VanillaContainerSkin.LABEL_COLOR, false);
        Component balance = Component.translatable("container.cointcore.trader.balance", menu.gluonBalance());
        graphics.drawString(font, balance, titleLabelX, 16, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, titleLabelX + font.width(balance) + 2, 15);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);

        int start = page * TraderMenu.PAGE_SIZE;
        for (int row = 0; row < TraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            TraderOffer offer = menu.offers().get(offerIndex);
            int y = TraderMenu.TITLE_HEIGHT + 3 + row * TraderMenu.ROW_HEIGHT;
            ItemStack stack = offer.display();
            graphics.drawString(
                    font,
                    ellipsize(stack.getHoverName().getString(), textMaxWidth),
                    TEXT_LEFT,
                    y,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            Component prices = Component.translatable(
                    "gui.cointcore.trader.prices",
                    offer.canBuy() ? Long.toString(offer.buyTotal()) : "—",
                    offer.canSell() ? Long.toString(offer.sellNet()) : "—"
            );
            Component priceLine = ellipsize(prices.getString(), textMaxWidth);
            graphics.drawString(
                    font,
                    priceLine,
                    TEXT_LEFT,
                    y + 11,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            GluonGuiIcon.blit(graphics, TEXT_LEFT + font.width(priceLine) + 2, y + 10);
        }

        if (menu.offers().isEmpty()) {
            Component empty = Component.translatable("gui.cointcore.trader.empty");
            graphics.drawString(
                    font,
                    empty,
                    (imageWidth - font.width(empty)) / 2,
                    TraderMenu.TITLE_HEIGHT + 20,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
        }

        int pages = maxPage() + 1;
        Component pageLabel = Component.translatable("gui.cointcore.trader.page", page + 1, pages);
        int pagerY = TraderMenu.OFFER_PANEL_HEIGHT - 13;
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
            int statusY = TraderMenu.TITLE_HEIGHT + TraderMenu.PAGE_SIZE * TraderMenu.ROW_HEIGHT + 1;
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
        int start = page * TraderMenu.PAGE_SIZE;
        int rowLeft = x + 7;
        int rowWidth = imageWidth - 14;
        for (int row = 0; row < TraderMenu.PAGE_SIZE && start + row < menu.offers().size(); row++) {
            int rowY = y + TraderMenu.TITLE_HEIGHT + 2 + row * TraderMenu.ROW_HEIGHT;
            VanillaContainerSkin.blitOfferRow(graphics, rowLeft, rowY, rowWidth, TraderMenu.ROW_HEIGHT - 2);
            VanillaContainerSkin.blitSlot(graphics, x + 7, rowY);
        }
        VanillaContainerSkin.blitMenuSlots(graphics, x, y, menu.slots);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth
                && mouseY >= topPos + TraderMenu.TITLE_HEIGHT
                && mouseY < topPos + TraderMenu.OFFER_PANEL_HEIGHT) {
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
        int start = page * TraderMenu.PAGE_SIZE;
        for (int row = 0; row < TraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.offers().size()) {
                break;
            }
            ItemStack stack = menu.offers().get(offerIndex).display();
            int itemX = leftPos + ICON_LEFT;
            int itemY = topPos + TraderMenu.TITLE_HEIGHT + 3 + row * TraderMenu.ROW_HEIGHT;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private void renderSparklines(GuiGraphics graphics) {
        int start = page * TraderMenu.PAGE_SIZE;
        double band = TraderOffersConfig.priceHistoryAverageBandPercent();
        for (int row = 0; row < TraderMenu.PAGE_SIZE; row++) {
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
        int rowY = topPos + TraderMenu.TITLE_HEIGHT + 2 + row * TraderMenu.ROW_HEIGHT;
        return rowY + (TraderMenu.ROW_HEIGHT - 2 - OfferPriceSparkline.HEIGHT) / 2;
    }

    private int hoveredChartOffer(int mouseX, int mouseY) {
        int start = page * TraderMenu.PAGE_SIZE;
        int chartX = chartScreenX();
        for (int row = 0; row < TraderMenu.PAGE_SIZE; row++) {
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
        return (size - 1) / TraderMenu.PAGE_SIZE;
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
        int localY = mouseY - topPos - TraderMenu.TITLE_HEIGHT;
        if (localY < 2) {
            return -1;
        }
        int row = (localY - 2) / TraderMenu.ROW_HEIGHT;
        if (row < 0 || row >= TraderMenu.PAGE_SIZE) {
            return -1;
        }
        int rowTop = 2 + row * TraderMenu.ROW_HEIGHT;
        if (localY < rowTop || localY >= rowTop + ICON_SIZE) {
            return -1;
        }
        if (localX < ICON_LEFT - 1 || localX >= TEXT_LEFT + textMaxWidth) {
            return -1;
        }
        int offerIndex = page * TraderMenu.PAGE_SIZE + row;
        if (offerIndex >= menu.offers().size()) {
            return -1;
        }
        return offerIndex;
    }

    private boolean hoveredIcon(int mouseX, int mouseY, int offerIndex) {
        int row = offerIndex - page * TraderMenu.PAGE_SIZE;
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - TraderMenu.TITLE_HEIGHT;
        int rowTop = 2 + row * TraderMenu.ROW_HEIGHT;
        return localX >= ICON_LEFT - 1 && localX < ICON_LEFT + ICON_SIZE
                && localY >= rowTop && localY < rowTop + ICON_SIZE;
    }

    private boolean hoveredName(int mouseX, int mouseY, int offerIndex) {
        int row = offerIndex - page * TraderMenu.PAGE_SIZE;
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - TraderMenu.TITLE_HEIGHT;
        int rowTop = 2 + row * TraderMenu.ROW_HEIGHT;
        return localX >= TEXT_LEFT && localX < TEXT_LEFT + textMaxWidth
                && localY >= rowTop && localY < rowTop + 10;
    }

    private Component feedbackLine(TraderFeedbackPayload payload) {
        TraderFeedbackKind kind = payload.kind();
        return switch (kind) {
            case OFFER_UNAVAILABLE -> Component.translatable("gui.cointcore.trader.error.offer_unavailable");
            case INVENTORY_FULL -> Component.translatable("gui.cointcore.trader.error.inventory_full");
            case NOT_ENOUGH_GLUONS -> Component.translatable("gui.cointcore.trader.error.not_enough", payload.gluons());
            case NOT_ENOUGH_ITEMS -> Component.translatable("gui.cointcore.trader.error.not_enough_items");
            case BOUGHT -> Component.translatable(
                    "gui.cointcore.trader.bought",
                    payload.count(),
                    payload.itemName(),
                    payload.gluons(),
                    payload.fee()
            );
            case SOLD -> Component.translatable(
                    "gui.cointcore.trader.sold",
                    payload.count(),
                    payload.itemName(),
                    payload.gluons(),
                    payload.fee()
            );
        };
    }
}
