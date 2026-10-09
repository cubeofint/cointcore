package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.shop.PlayerTraderListing;
import com.mawlee.cointcore.shop.PlayerTraderManageLayout;
import com.mawlee.cointcore.shop.PlayerTraderMenu;
import com.mawlee.cointcore.shop.PlayerTraderTabPayload;
import com.mawlee.cointcore.shop.TraderFeedbackLines;
import com.mawlee.cointcore.shop.TraderFeedbackPayload;
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

public class PlayerTraderScreen extends AbstractContainerScreen<PlayerTraderMenu> {
    private static final int TEXT_LEFT = 28;
    private static final int ICON_LEFT = 8;
    private static final int ICON_SIZE = 18;
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 2;
    private static final int NAME_OFFSET = 2;
    private static final int PRICE_BLOCK_TOP = 14;
    private static final String ELLIPSIS = "…";

    private int page;
    private int buttonWidth;
    private int buttonColumnX;
    private int textMaxWidth;
    private TraderFeedbackPayload feedback;

    public PlayerTraderScreen(PlayerTraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PlayerTraderMenu.GUI_WIDTH;
        this.imageHeight = PlayerTraderMenu.GUI_HEIGHT;
        this.inventoryLabelX = PlayerTraderMenu.PLAYER_INV_LEFT;
        this.inventoryLabelY = PlayerTraderMenu.PLAYER_INV_Y - 12;
    }

    void setFeedback(TraderFeedbackPayload payload) {
        this.feedback = payload;
    }

    void onCatalogUpdated() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        page = Math.min(page, maxPage());
        buttonWidth = measureButtonWidth();
        buttonColumnX = imageWidth - 8 - buttonWidth;
        textMaxWidth = Math.max(16, buttonColumnX - TEXT_LEFT - 8);
        int listLeft = leftPos + 8;
        if (menu.canManage()) {
            Component shopTab = Component.translatable("gui.cointcore.player_trader.tab.shop");
            Component manageTab = Component.translatable("gui.cointcore.player_trader.tab.manage");
            int shopTabW = PlayerTraderManageLayout.tabWidth(font.width(shopTab));
            int manageTabW = PlayerTraderManageLayout.tabWidth(font.width(manageTab));
            addRenderableWidget(Button.builder(shopTab, button -> {
            }).bounds(listLeft, topPos + 4, shopTabW, 16).build()).active = false;
            addRenderableWidget(Button.builder(
                    manageTab,
                    button -> PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, true))
            ).bounds(listLeft + shopTabW + 4, topPos + 4, manageTabW, 16).build());
        }

        int buttonY0 = topPos + PlayerTraderMenu.TITLE_HEIGHT + 2;
        int start = page * PlayerTraderMenu.PAGE_SIZE;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.listings().size()) {
                break;
            }
            PlayerTraderListing listing = menu.listings().get(offerIndex);
            TraderOffer offer = listing.offer();
            int y = buttonY0 + row * PlayerTraderMenu.ROW_HEIGHT + PRICE_BLOCK_TOP;
            int buyX = leftPos + buttonColumnX;
            Button buy = Button.builder(Component.translatable("gui.cointcore.trader.buy"), button -> trade(offerIndex, false))
                    .bounds(buyX, y, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.trader.shift_hint")))
                    .build();
            buy.active = offer.canBuy() && listing.inStock();
            addRenderableWidget(buy);
            Button sell = Button.builder(Component.translatable("gui.cointcore.trader.sell"), button -> trade(offerIndex, true))
                    .bounds(buyX, y + BUTTON_HEIGHT + BUTTON_GAP, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.trader.shift_hint")))
                    .build();
            sell.active = offer.canSell();
            addRenderableWidget(sell);
        }

        int pagerY = topPos + PlayerTraderMenu.OFFER_PANEL_HEIGHT - PlayerTraderMenu.PAGE_BAR_HEIGHT + 1;
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOfferItems(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, menu.canManage() ? 22 : 6, VanillaContainerSkin.LABEL_COLOR, false);
        Component balance = Component.translatable("container.cointcore.trader.balance", menu.gluonBalance());
        int balanceY = menu.canManage() ? 32 : 16;
        graphics.drawString(font, balance, titleLabelX, balanceY, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, titleLabelX + font.width(balance) + 2, balanceY - 1);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);

        int start = page * PlayerTraderMenu.PAGE_SIZE;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.listings().size()) {
                break;
            }
            PlayerTraderListing listing = menu.listings().get(offerIndex);
            TraderOffer offer = listing.offer();
            int y = PlayerTraderMenu.TITLE_HEIGHT + 2 + row * PlayerTraderMenu.ROW_HEIGHT;
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
            if (!listing.inStock() && offer.canBuy()) {
                graphics.drawString(
                        font,
                        Component.translatable("gui.cointcore.player_trader.out_of_stock"),
                        TEXT_LEFT,
                        buyY,
                        0xAA0000,
                        false
                );
            } else {
                drawPriceLine(
                        graphics,
                        Component.translatable(
                                "gui.cointcore.trader.buy_price",
                                offer.canBuy() ? Long.toString(offer.buyTotal()) : "—"
                        ),
                        buyY
                );
            }
            drawPriceLine(
                    graphics,
                    Component.translatable(
                            "gui.cointcore.trader.sell_price",
                            offer.canSell() ? Long.toString(offer.sellNet()) : "—"
                    ),
                    sellY
            );
        }

        if (menu.listings().isEmpty()) {
            Component empty = Component.translatable("gui.cointcore.player_trader.empty");
            graphics.drawString(
                    font,
                    empty,
                    (imageWidth - font.width(empty)) / 2,
                    PlayerTraderMenu.TITLE_HEIGHT + 20,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
        }

        int pages = maxPage() + 1;
        Component pageLabel = Component.translatable("gui.cointcore.trader.page", page + 1, pages);
        int pagerY = PlayerTraderMenu.OFFER_PANEL_HEIGHT - 13;
        graphics.drawString(
                font,
                pageLabel,
                (imageWidth - font.width(pageLabel)) / 2,
                pagerY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );

        if (feedback != null) {
            Component line = TraderFeedbackLines.of(feedback);
            int color = feedback.kind().error() ? 0xAA0000 : 0x2E7D32;
            int statusY = PlayerTraderMenu.TITLE_HEIGHT + PlayerTraderMenu.PAGE_SIZE * PlayerTraderMenu.ROW_HEIGHT + 1;
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
        int offerIndex = hoveredOffer(mouseX, mouseY);
        if (offerIndex >= 0) {
            TraderOffer offer = menu.listings().get(offerIndex).offer();
            if (hoveredIcon(mouseX, mouseY, offerIndex)) {
                graphics.renderTooltip(font, offer.display(), mouseX, mouseY);
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
        int start = page * PlayerTraderMenu.PAGE_SIZE;
        int rowLeft = x + 7;
        int rowWidth = imageWidth - 14;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE && start + row < menu.listings().size(); row++) {
            int rowY = y + PlayerTraderMenu.TITLE_HEIGHT + 2 + row * PlayerTraderMenu.ROW_HEIGHT;
            VanillaContainerSkin.blitOfferRow(graphics, rowLeft, rowY, rowWidth, PlayerTraderMenu.ROW_HEIGHT - 2);
            VanillaContainerSkin.blitSlot(graphics, x + 7, rowY);
        }
        VanillaContainerSkin.blitMenuSlots(graphics, x, y, menu.slots);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth
                && mouseY >= topPos + PlayerTraderMenu.TITLE_HEIGHT
                && mouseY < topPos + PlayerTraderMenu.OFFER_PANEL_HEIGHT) {
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
        int start = page * PlayerTraderMenu.PAGE_SIZE;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= menu.listings().size()) {
                break;
            }
            ItemStack stack = menu.listings().get(offerIndex).offer().display();
            int itemX = leftPos + ICON_LEFT;
            int itemY = topPos + PlayerTraderMenu.TITLE_HEIGHT + 3 + row * PlayerTraderMenu.ROW_HEIGHT;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private void drawPriceLine(GuiGraphics graphics, Component line, int y) {
        graphics.drawString(font, line, TEXT_LEFT, y, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, TEXT_LEFT + font.width(line) + 2, y - 1);
    }

    private int priceTextOffset() {
        return Math.max(0, (BUTTON_HEIGHT - 8) / 2);
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
        int size = menu.listings().size();
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / PlayerTraderMenu.PAGE_SIZE;
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
        int localY = mouseY - topPos - PlayerTraderMenu.TITLE_HEIGHT;
        if (localY < 2) {
            return -1;
        }
        int row = (localY - 2) / PlayerTraderMenu.ROW_HEIGHT;
        if (row < 0 || row >= PlayerTraderMenu.PAGE_SIZE) {
            return -1;
        }
        int offerIndex = page * PlayerTraderMenu.PAGE_SIZE + row;
        if (offerIndex >= menu.listings().size()) {
            return -1;
        }
        if (localX < ICON_LEFT - 1 || localX >= ICON_LEFT + ICON_SIZE) {
            return -1;
        }
        return offerIndex;
    }

    private boolean hoveredIcon(int mouseX, int mouseY, int offerIndex) {
        return hoveredOffer(mouseX, mouseY) == offerIndex;
    }
}
