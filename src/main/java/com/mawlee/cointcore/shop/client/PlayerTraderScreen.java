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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlayerTraderScreen extends AbstractContainerScreen<PlayerTraderMenu> {
    private static final int TEXT_LEFT = 28;
    private static final int ICON_LEFT = 8;
    private static final int ICON_SIZE = 18;
    private static final int BUTTON_PADDING = 16;
    private static final int BUTTON_HEIGHT = 16;
    private static final int NAME_OFFSET = 2;
    private static final int PRICE_BLOCK_TOP = 14;
    private static final String ELLIPSIS = "…";

    private int page;
    private int buttonWidth;
    private int buttonColumnX;
    private int textMaxWidth;
    private TraderFeedbackPayload feedback;
    private EditBox searchBox;
    private String query = "";
    private String modFilter = "";
    private boolean sortByNew;
    private List<PlayerTraderListing> view = List.of();

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
        view = visibleListings();
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
            }).bounds(listLeft, topPos + 4, shopTabW, 16).build()).active = false;
            addRenderableWidget(Button.builder(
                    manageTab,
                    button -> PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, true))
            ).bounds(listLeft + shopTabW + 4, topPos + 4, manageTabW, 16).build());
        }

        searchBox = new EditBox(font, listLeft, topPos + 22, 120, 12, Component.translatable("gui.cointcore.player_trader.search"));
        searchBox.setValue(query);
        searchBox.setResponder(text -> {
            query = text == null ? "" : text;
            page = 0;
            view = visibleListings();
        });
        addRenderableWidget(searchBox);

        Component sortLabel = Component.translatable(
                sortByNew ? "gui.cointcore.player_trader.sort.new" : "gui.cointcore.player_trader.sort.price"
        );
        int sortW = Math.max(54, font.width(sortLabel) + 12);
        addRenderableWidget(Button.builder(sortLabel, button -> {
            sortByNew = !sortByNew;
            rebuildWidgets();
        }).bounds(listLeft + 124, topPos + 20, sortW, 14).build());

        List<String> mods = distinctMods();
        if (!mods.isEmpty()) {
            String shown = modFilter.isBlank() ? "all" : modFilter;
            Component modLabel = Component.translatable("gui.cointcore.player_trader.filter.mod", shown);
            int modW = Math.max(54, font.width(modLabel) + 12);
            addRenderableWidget(Button.builder(modLabel, button -> {
                cycleMod(mods);
                rebuildWidgets();
            }).bounds(leftPos + imageWidth - 8 - modW, topPos + 20, modW, 14).build());
        }

        int buttonY0 = topPos + PlayerTraderMenu.TITLE_HEIGHT + 2;
        int start = page * PlayerTraderMenu.PAGE_SIZE;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= view.size()) {
                break;
            }
            PlayerTraderListing listing = view.get(offerIndex);
            TraderOffer offer = listing.offer();
            int y = buttonY0 + row * PlayerTraderMenu.ROW_HEIGHT + PRICE_BLOCK_TOP;
            int buyX = leftPos + buttonColumnX;
            int catalogIndex = catalogIndexOf(listing);
            Button buy = Button.builder(Component.translatable("gui.cointcore.trader.buy"), button -> trade(catalogIndex))
                    .bounds(buyX, y, buttonWidth, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.player_trader.shift_hint")))
                    .build();
            buy.active = offer.canBuy() && listing.inStock() && catalogIndex >= 0;
            addRenderableWidget(buy);
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
        int balanceY = 36;
        int balanceX = imageWidth - 10 - GluonGuiIcon.SIZE - font.width(balance);
        graphics.drawString(font, balance, balanceX, balanceY, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, balanceX + font.width(balance) + 2, balanceY - 1);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);

        int start = page * PlayerTraderMenu.PAGE_SIZE;
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE; row++) {
            int offerIndex = start + row;
            if (offerIndex >= view.size()) {
                break;
            }
            PlayerTraderListing listing = view.get(offerIndex);
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
            int buyY = y + PRICE_BLOCK_TOP + Math.max(0, (BUTTON_HEIGHT - 8) / 2);
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
                Component price = Component.translatable(
                        "gui.cointcore.player_trader.best_price",
                        offer.canBuy() ? Long.toString(offer.buyTotal()) : "—",
                        Integer.toString(listing.dealsLeft())
                );
                graphics.drawString(font, price, TEXT_LEFT, buyY, VanillaContainerSkin.LABEL_COLOR, false);
                GluonGuiIcon.blit(graphics, TEXT_LEFT + font.width(price) + 2, buyY - 1);
            }
        }

        if (view.isEmpty()) {
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

        Component pageLabel = Component.translatable("gui.cointcore.trader.page", page + 1, maxPage() + 1);
        graphics.drawString(
                font,
                pageLabel,
                (imageWidth - font.width(pageLabel)) / 2,
                PlayerTraderMenu.OFFER_PANEL_HEIGHT - 13,
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
            graphics.renderTooltip(font, view.get(offerIndex).offer().display(), mouseX, mouseY);
            return;
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
        for (int row = 0; row < PlayerTraderMenu.PAGE_SIZE && start + row < view.size(); row++) {
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
            if (offerIndex >= view.size()) {
                break;
            }
            ItemStack stack = view.get(offerIndex).offer().display();
            int itemX = leftPos + ICON_LEFT;
            int itemY = topPos + PlayerTraderMenu.TITLE_HEIGHT + 3 + row * PlayerTraderMenu.ROW_HEIGHT;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
        }
    }

    private List<PlayerTraderListing> visibleListings() {
        List<PlayerTraderListing> filtered = new ArrayList<>();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (PlayerTraderListing listing : menu.listings()) {
            if (!modFilter.isBlank() && !modFilter.equals(listing.modId())) {
                continue;
            }
            String name = listing.offer().display().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty() && !name.contains(needle)) {
                continue;
            }
            filtered.add(listing);
        }
        Map<String, PlayerTraderListing> grouped = new LinkedHashMap<>();
        for (PlayerTraderListing listing : filtered) {
            String key = groupKey(listing);
            PlayerTraderListing current = grouped.get(key);
            if (current == null || listing.offer().buyPrice() < current.offer().buyPrice()) {
                int deals = listing.dealsLeft() + (current == null ? 0 : current.dealsLeft());
                grouped.put(key, new PlayerTraderListing(
                        listing.listingId(),
                        listing.offer(),
                        deals,
                        listing.sellerName(),
                        listing.sellerId(),
                        listing.createdAt(),
                        listing.modId()
                ));
            } else {
                grouped.put(key, new PlayerTraderListing(
                        current.listingId(),
                        current.offer(),
                        current.dealsLeft() + listing.dealsLeft(),
                        current.sellerName(),
                        current.sellerId(),
                        current.createdAt(),
                        current.modId()
                ));
            }
        }
        List<PlayerTraderListing> rows = new ArrayList<>(grouped.values());
        if (sortByNew) {
            rows.sort(Comparator.comparingLong(PlayerTraderListing::createdAt).reversed());
        } else {
            rows.sort(Comparator.comparingLong((PlayerTraderListing row) -> row.offer().buyPrice()));
        }
        return rows;
    }

    private static String groupKey(PlayerTraderListing listing) {
        ItemStack stack = listing.offer().display();
        return stack.getItemHolder().getRegisteredName()
                + "|"
                + listing.offer().count()
                + "|"
                + stack.getComponents().toString();
    }

    private List<String> distinctMods() {
        List<String> mods = new ArrayList<>();
        for (PlayerTraderListing listing : menu.listings()) {
            if (!listing.modId().isBlank() && !mods.contains(listing.modId())) {
                mods.add(listing.modId());
            }
        }
        return mods;
    }

    private void cycleMod(List<String> mods) {
        if (modFilter.isBlank()) {
            modFilter = mods.getFirst();
            return;
        }
        int index = mods.indexOf(modFilter);
        if (index < 0 || index + 1 >= mods.size()) {
            modFilter = "";
            return;
        }
        modFilter = mods.get(index + 1);
    }

    private int catalogIndexOf(PlayerTraderListing listing) {
        for (int index = 0; index < menu.listings().size(); index++) {
            if (menu.listings().get(index).listingId().equals(listing.listingId())) {
                return index;
            }
        }
        return -1;
    }

    private void trade(int offerIndex) {
        if (offerIndex < 0) {
            return;
        }
        PacketDistributor.sendToServer(new TraderTradePayload(menu.containerId, offerIndex, false, hasShiftDown()));
    }

    private void changePage(int delta) {
        view = visibleListings();
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
        return (size - 1) / PlayerTraderMenu.PAGE_SIZE;
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
        int localY = mouseY - topPos - PlayerTraderMenu.TITLE_HEIGHT;
        if (localY < 2) {
            return -1;
        }
        int row = (localY - 2) / PlayerTraderMenu.ROW_HEIGHT;
        if (row < 0 || row >= PlayerTraderMenu.PAGE_SIZE) {
            return -1;
        }
        int offerIndex = page * PlayerTraderMenu.PAGE_SIZE + row;
        if (offerIndex >= view.size()) {
            return -1;
        }
        if (localX < ICON_LEFT - 1 || localX >= ICON_LEFT + ICON_SIZE) {
            return -1;
        }
        return offerIndex;
    }
}
