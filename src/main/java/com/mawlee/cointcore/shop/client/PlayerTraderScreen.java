package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.shop.GlobalMarketMath;
import com.mawlee.cointcore.shop.GlobalMarketRows;
import com.mawlee.cointcore.shop.OfferPriceVerdict;
import com.mawlee.cointcore.shop.PlayerTraderListing;
import com.mawlee.cointcore.shop.PlayerTraderManageLayout;
import com.mawlee.cointcore.shop.PlayerTraderMenu;
import com.mawlee.cointcore.shop.PlayerTraderTabPayload;
import com.mawlee.cointcore.shop.TraderFeedbackLines;
import com.mawlee.cointcore.shop.TraderFeedbackPayload;
import com.mawlee.cointcore.shop.TraderOffer;
import com.mawlee.cointcore.shop.TraderShopLayout;
import com.mawlee.cointcore.shop.TraderTradePayload;
import com.mawlee.cointcore.shop.jei.JeiSearchBridge;
import com.mawlee.cointcore.ui.ScaledGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PlayerTraderScreen extends AbstractContainerScreen<PlayerTraderMenu> {
    private static final int TEXT_LEFT = 28;
    private static final int ICON_LEFT = 8;
    private static final int ICON_SIZE = 18;
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT = 16;
    private static final int NAME_OFFSET = 2;
    private static final int PRICE_BLOCK_TOP = 14;
    private static final String ELLIPSIS = "…";

    private TraderShopLayout layout = TraderShopLayout.preferredMarket(true);
    private int page;
    private int buttonWidth;
    private int buttonColumnX;
    private int textMaxWidth;
    private TraderFeedbackPayload feedback;
    private EditBox searchBox;
    private String query = "";
    private boolean sortByNew;
    private boolean jeiSeeded;
    private boolean syncingJei;
    private boolean searchDirty;
    private List<GlobalMarketRows.Row> view = List.of();

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
        layout = TraderShopLayout.market(width, height, menu.canManage());
        imageWidth = layout.guiWidth();
        imageHeight = layout.guiHeight();
        inventoryLabelX = layout.playerInvLeft();
        inventoryLabelY = layout.playerInvY() - 12;
        GuiSlotMover.movePlayerInventory(menu.slots, 0, layout.playerInvLeft(), layout.playerInvY());
        super.init();
        leftPos = ScaledGuiLayout.origin(width, imageWidth, 0, 0, ScaledGuiLayout.MARGIN);
        topPos = ScaledGuiLayout.origin(height, imageHeight, 0, 0, ScaledGuiLayout.MARGIN);
        seedQueryFromJei();
        view = visibleRows();
        page = Math.min(page, maxPage());
        buttonWidth = measureButtonWidth();
        buttonColumnX = imageWidth - 8 - buttonWidth;
        textMaxWidth = Math.max(16, buttonColumnX - TEXT_LEFT - 8);
        int listLeft = leftPos + 8;
        if (menu.canManage()) {
            Component shopTab = Component.translatable("gui.cointcore.player_trader.tab.shop");
            Component manageTab = Component.translatable("gui.cointcore.player_trader.tab.mine");
            int shopTabW = PlayerTraderManageLayout.tabWidth(font.width(shopTab));
            int manageTabW = PlayerTraderManageLayout.tabWidth(font.width(manageTab));
            addRenderableWidget(Button.builder(shopTab, button -> {
            }).bounds(listLeft, topPos + layout.tabY(), shopTabW, 16).build()).active = false;
            addRenderableWidget(Button.builder(
                    manageTab,
                    button -> PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, true))
            ).bounds(listLeft + shopTabW + 4, topPos + layout.tabY(), manageTabW, 16).build());
        }

        int searchW = Math.min(180, Math.max(80, imageWidth / 2));
        searchBox = new EditBox(
                font,
                listLeft,
                topPos + layout.searchY(),
                searchW,
                12,
                Component.translatable("gui.cointcore.player_trader.search")
        );
        searchBox.setHint(Component.translatable("gui.cointcore.player_trader.search"));
        searchBox.setTooltip(Tooltip.create(Component.translatable("gui.cointcore.player_trader.search.hint")));
        searchBox.setValue(query);
        searchBox.setResponder(this::onSearchTyped);
        addRenderableWidget(searchBox);

        Component sortLabel = Component.translatable(
                sortByNew ? "gui.cointcore.player_trader.sort.new" : "gui.cointcore.player_trader.sort.price"
        );
        int sortW = Math.max(54, font.width(sortLabel) + 12);
        int sortX = listLeft + searchW + 4;
        addRenderableWidget(Button.builder(sortLabel, button -> {
            sortByNew = !sortByNew;
            rebuildWidgets();
        }).bounds(sortX, topPos + layout.searchY() - 2, sortW, 14).build());

        int buttonY0 = topPos + layout.listTop();
        int start = page * layout.pageSize();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= view.size()) {
                break;
            }
            GlobalMarketRows.Row listing = view.get(offerIndex);
            int y = buttonY0 + row * layout.rowHeight() + PRICE_BLOCK_TOP;
            int buyX = leftPos + buttonColumnX;
            int catalogIndex = catalogIndexOf(listing.listingId());
            Button buy = Button.builder(Component.translatable("gui.cointcore.trader.buy"), button -> trade(catalogIndex))
                    .bounds(buyX, y, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.player_trader.shift_hint")))
                    .build();
            buy.active = listing.dealsLeft() > 0 && listing.price() > 0L && catalogIndex >= 0;
            addRenderableWidget(buy);
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOfferItems(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawHeaderTitle(graphics);
        Component balance = Component.translatable("container.cointcore.trader.balance", menu.gluonBalance());
        int balanceY = layout.balanceY();
        int balanceX = imageWidth - 10 - GluonGuiIcon.SIZE - font.width(balance);
        graphics.drawString(font, balance, balanceX, balanceY, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, balanceX + font.width(balance) + 2, balanceY - 1);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);

        int start = page * layout.pageSize();
        for (int row = 0; row < layout.pageSize(); row++) {
            int offerIndex = start + row;
            if (offerIndex >= view.size()) {
                break;
            }
            GlobalMarketRows.Row listing = view.get(offerIndex);
            PlayerTraderListing catalog = catalogOf(listing.listingId());
            int y = layout.rowY(row);
            ItemStack stack = catalog == null ? ItemStack.EMPTY : catalog.offer().display();
            graphics.drawString(
                    font,
                    ellipsize(stack.getHoverName().getString(), textMaxWidth),
                    TEXT_LEFT,
                    y + NAME_OFFSET,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            int buyY = y + PRICE_BLOCK_TOP + Math.max(0, (BUTTON_HEIGHT - 8) / 2);
            if (listing.dealsLeft() < 1) {
                graphics.drawString(
                        font,
                        Component.translatable("gui.cointcore.player_trader.out_of_stock"),
                        TEXT_LEFT,
                        buyY,
                        0xAA0000,
                        false
                );
            } else {
                Component price = Component.translatable(
                        "gui.cointcore.player_trader.lot_price",
                        Integer.toString(listing.count()),
                        Long.toString(listing.price())
                );
                graphics.drawString(font, price, TEXT_LEFT, buyY, VanillaContainerSkin.LABEL_COLOR, false);
                int cursor = TEXT_LEFT + font.width(price) + 2;
                GluonGuiIcon.blit(graphics, cursor, buyY - 1);
                cursor += GluonGuiIcon.SIZE + 6;
                Component lots = Component.translatable(
                        "gui.cointcore.player_trader.lot_count",
                        Integer.toString(listing.dealsLeft())
                );
                graphics.drawString(font, lots, cursor, buyY, VanillaContainerSkin.LABEL_COLOR, false);
                cursor += font.width(lots) + 6;
                Component seller = listing.multiSeller()
                        ? Component.translatable("gui.cointcore.player_trader.sellers", listing.sellers().size())
                        : Component.translatable("gui.cointcore.player_trader.seller", listing.cheapestSeller());
                graphics.drawString(
                        font,
                        ellipsize(seller.getString(), Math.max(16, buttonColumnX - cursor - 36)),
                        cursor,
                        buyY,
                        VanillaContainerSkin.LABEL_COLOR,
                        false
                );
                drawVerdict(graphics, listing, y + NAME_OFFSET);
            }
        }

        if (view.isEmpty()) {
            Component empty = Component.translatable("gui.cointcore.player_trader.empty");
            graphics.drawString(
                    font,
                    empty,
                    (imageWidth - font.width(empty)) / 2,
                    layout.titleHeight() + 20,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
        }

        Component pageLabel = Component.translatable("gui.cointcore.trader.page", page + 1, maxPage() + 1);
        graphics.drawString(
                font,
                pageLabel,
                (imageWidth - font.width(pageLabel)) / 2,
                layout.offerPanelHeight() - 13,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );

        if (feedback != null) {
            Component line = TraderFeedbackLines.of(feedback);
            int color = feedback.kind().error() ? 0xAA0000 : 0x2E7D32;
            graphics.drawString(
                    font,
                    Component.literal(font.plainSubstrByWidth(line.getString(), imageWidth - 16)),
                    8,
                    layout.statusY(),
                    color,
                    false
            );
        }
    }

    private void drawHeaderTitle(GuiGraphics graphics) {
        if (menu.canManage()) {
            Component shopTab = Component.translatable("gui.cointcore.player_trader.tab.shop");
            Component manageTab = Component.translatable("gui.cointcore.player_trader.tab.mine");
            int tabsW = PlayerTraderManageLayout.tabWidth(font.width(shopTab))
                    + 4
                    + PlayerTraderManageLayout.tabWidth(font.width(manageTab));
            int titleX = 8 + tabsW + 8;
            int max = Math.max(16, imageWidth / 2 - titleX);
            graphics.drawString(
                    font,
                    ellipsize(title.getString(), max),
                    titleX,
                    layout.titleY(),
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            return;
        }
        graphics.drawString(
                font,
                ellipsize(title.getString(), imageWidth - 16),
                titleLabelX,
                layout.titleY(),
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
    }

    private void drawVerdict(GuiGraphics graphics, GlobalMarketRows.Row listing, int y) {
        OfferPriceVerdict verdict = OfferPriceVerdict.classify(
                listing.unitPrice(),
                listing.marketUnitPrice(),
                TraderOffersConfig.priceHistoryAverageBandPercent()
        );
        if (verdict == OfferPriceVerdict.UNKNOWN) {
            return;
        }
        Component label = switch (verdict) {
            case CHEAP -> Component.translatable("gui.cointcore.player_trader.verdict.cheap");
            case FAIR -> Component.translatable("gui.cointcore.player_trader.verdict.fair");
            case EXPENSIVE -> Component.translatable("gui.cointcore.player_trader.verdict.expensive");
            case UNKNOWN -> Component.empty();
        };
        int color = OfferPriceSparkline.color(verdict) & 0x00FFFFFF;
        graphics.drawString(
                font,
                label,
                buttonColumnX - 4 - font.width(label),
                y,
                color,
                false
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (searchDirty) {
            searchDirty = false;
            refreshSearchResults();
        }
        if (searchBox == null || syncingJei || !JeiSearchBridge.available()) {
            return;
        }
        String jei = JeiSearchBridge.getFilterText();
        if (jei == null || jei.equals(query)) {
            return;
        }
        applyExternalQuery(jei);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE
                && keyCode != GLFW.GLFW_KEY_TAB
                && getFocused() instanceof EditBox box
                && box.canConsumeInput()) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int offerIndex = hoveredOffer(mouseX, mouseY);
        if (offerIndex >= 0) {
            GlobalMarketRows.Row row = view.get(offerIndex);
            PlayerTraderListing catalog = catalogOf(row.listingId());
            if (hoveredIcon(mouseX, mouseY, offerIndex) && catalog != null) {
                graphics.renderTooltip(font, catalog.offer().display(), mouseX, mouseY);
                return;
            }
            graphics.renderComponentTooltip(font, lotTooltip(row), mouseX, mouseY);
            return;
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
        for (int row = 0; row < layout.pageSize() && start + row < view.size(); row++) {
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
            if (offerIndex >= view.size()) {
                break;
            }
            PlayerTraderListing catalog = catalogOf(view.get(offerIndex).listingId());
            if (catalog == null) {
                continue;
            }
            ItemStack stack = catalog.offer().display();
            int itemX = leftPos + ICON_LEFT;
            int itemY = topPos + layout.rowY(row) + 1;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private List<GlobalMarketRows.Row> visibleRows() {
        List<GlobalMarketRows.Source> filtered = new ArrayList<>();
        for (PlayerTraderListing listing : menu.listings()) {
            if (!MarketSearch.matches(query, listing.offer().display(), listing.modId())) {
                continue;
            }
            filtered.add(toSource(listing));
        }
        return GlobalMarketRows.group(filtered, sortByNew);
    }

    private void seedQueryFromJei() {
        if (jeiSeeded) {
            return;
        }
        jeiSeeded = true;
        if (query != null && !query.isEmpty()) {
            return;
        }
        String jei = JeiSearchBridge.getFilterText();
        if (jei != null && !jei.isEmpty()) {
            query = jei;
        }
    }

    private void onSearchTyped(String text) {
        if (syncingJei) {
            return;
        }
        String next = text == null ? "" : text;
        if (next.equals(query)) {
            return;
        }
        query = next;
        page = 0;
        JeiSearchBridge.setFilterText(query);
        view = visibleRows();
        searchDirty = true;
    }

    private void applyExternalQuery(String text) {
        query = text == null ? "" : text;
        page = 0;
        syncingJei = true;
        try {
            refreshSearchResults();
        } finally {
            syncingJei = false;
        }
    }

    private void refreshSearchResults() {
        view = visibleRows();
        page = Math.min(page, maxPage());
        int cursor = searchBox == null ? 0 : searchBox.getCursorPosition();
        rebuildWidgets();
        if (searchBox != null) {
            searchBox.setCursorPosition(Math.min(cursor, searchBox.getValue().length()));
            setFocused(searchBox);
            searchBox.setFocused(true);
        }
    }

    private static GlobalMarketRows.Source toSource(PlayerTraderListing listing) {
        TraderOffer offer = listing.offer();
        ItemStack stack = offer.display();
        String key = stack.getItemHolder().getRegisteredName()
                + "|"
                + offer.count()
                + "|"
                + stack.getComponents().toString();
        return new GlobalMarketRows.Source(
                listing.listingId(),
                key,
                offer.count(),
                offer.buyPrice(),
                listing.dealsLeft(),
                listing.sellerName(),
                listing.createdAt(),
                listing.expiresAt(),
                listing.marketUnitPrice(),
                listing.modId()
        );
    }

    private int catalogIndexOf(String listingId) {
        for (int index = 0; index < menu.listings().size(); index++) {
            if (menu.listings().get(index).listingId().equals(listingId)) {
                return index;
            }
        }
        return -1;
    }

    private PlayerTraderListing catalogOf(String listingId) {
        int index = catalogIndexOf(listingId);
        return index < 0 ? null : menu.listings().get(index);
    }

    private void trade(int offerIndex) {
        if (offerIndex < 0) {
            return;
        }
        PacketDistributor.sendToServer(new TraderTradePayload(menu.containerId, offerIndex, false, hasShiftDown()));
    }

    private void changePage(int delta) {
        view = visibleRows();
        int next = Math.max(0, Math.min(maxPage(), page + delta));
        if (next != page) {
            page = next;
            rebuildWidgets();
        }
    }

    private int maxPage() {
        int size = view.size();
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / layout.pageSize();
    }

    private int measureButtonWidth() {
        return font.width(Component.translatable("gui.cointcore.trader.buy")) + BUTTON_PADDING;
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
        if (localY < 2 || localX < 7 || localX >= imageWidth - 7) {
            return -1;
        }
        int row = (localY - 2) / layout.rowHeight();
        if (row < 0 || row >= layout.pageSize()) {
            return -1;
        }
        int offerIndex = page * layout.pageSize() + row;
        if (offerIndex >= view.size()) {
            return -1;
        }
        return offerIndex;
    }

    private boolean hoveredIcon(int mouseX, int mouseY, int offerIndex) {
        int row = offerIndex - page * layout.pageSize();
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos - layout.rowY(row);
        return localX >= ICON_LEFT - 1 && localX < ICON_LEFT + ICON_SIZE
                && localY >= 0 && localY < ICON_SIZE;
    }

    private List<Component> lotTooltip(GlobalMarketRows.Row row) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "gui.cointcore.player_trader.tooltip.unit",
                Long.toString(row.unitPrice())
        ));
        if (row.marketUnitPrice() > 0L) {
            lines.add(Component.translatable(
                    "gui.cointcore.player_trader.tooltip.market",
                    Long.toString(row.marketUnitPrice())
            ));
        }
        if (row.multiSeller()) {
            lines.add(Component.translatable(
                    "gui.cointcore.player_trader.tooltip.sellers",
                    String.join(", ", row.sellers())
            ));
        } else if (!row.cheapestSeller().isBlank()) {
            lines.add(Component.translatable(
                    "gui.cointcore.player_trader.tooltip.seller",
                    row.cheapestSeller()
            ));
        }
        lines.add(expiresLine(row.expiresAt()));
        return lines;
    }

    private static Component expiresLine(long expiresAt) {
        if (expiresAt <= 0L) {
            return Component.translatable("gui.cointcore.player_trader.tooltip.expires_soon");
        }
        long left = expiresAt - System.currentTimeMillis();
        if (left < GlobalMarketMath.DAY_MS) {
            return Component.translatable("gui.cointcore.player_trader.tooltip.expires_soon");
        }
        long days = Math.max(1L, left / GlobalMarketMath.DAY_MS);
        return Component.translatable("gui.cointcore.player_trader.tooltip.expires", Long.toString(days));
    }
}
