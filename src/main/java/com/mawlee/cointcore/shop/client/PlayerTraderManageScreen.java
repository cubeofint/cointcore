package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.GuiEditBoxes;
import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.shop.GhostTemplate;
import com.mawlee.cointcore.shop.GlobalMarketPriceMath;
import com.mawlee.cointcore.shop.PlayerShopOfferSnapshot;
import com.mawlee.cointcore.shop.PlayerTraderGhostPayload;
import com.mawlee.cointcore.shop.PlayerTraderManageLayout;
import com.mawlee.cointcore.shop.PlayerTraderManageMenu;
import com.mawlee.cointcore.shop.PlayerTraderManagePayload;
import com.mawlee.cointcore.shop.PlayerTraderTabPayload;
import com.mawlee.cointcore.shop.TraderEditorSync;
import com.mawlee.cointcore.shop.TraderViewSync;
import com.mawlee.cointcore.ui.PagerChrome;
import com.mawlee.cointcore.ui.ScaledGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

public class PlayerTraderManageScreen extends AbstractContainerScreen<PlayerTraderManageMenu> {
    private PlayerTraderManageLayout.Geom geom = PlayerTraderManageLayout.fit(
            ScaledGuiLayout.SCALE3_1080P_W,
            ScaledGuiLayout.SCALE3_1080P_H
    );
    private EditBox countBox;
    private EditBox dealsBox;
    private EditBox priceBox;
    private Button deleteButton;
    private Button claimButton;
    private Button fillButton;
    private int lastOfferPagerMax;
    private int selected = -1;
    private int offerPage;
    private boolean deleteArmed;
    private Component status = Component.empty();
    private int statusColor = 0x2E7D32;
    private PlayerTraderManageLayout.RecommendPlacement recommendPlacement;
    private Component recommendFull = Component.empty();
    private boolean recommendTruncated;
    private String countText = "1";
    private String dealsText = "1";
    private String priceText = "1";
    private int restoreCursor = -1;
    private int restoreFocus = -1;

    public PlayerTraderManageScreen(PlayerTraderManageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PlayerTraderManageLayout.GUI_WIDTH;
        this.imageHeight = PlayerTraderManageLayout.GUI_HEIGHT;
        this.inventoryLabelX = PlayerTraderManageLayout.PAD;
        this.inventoryLabelY = PlayerTraderManageLayout.PLAYER_INV_LABEL_Y;
        this.titleLabelY = PlayerTraderManageLayout.TITLE_Y;
    }

    void onOffersUpdated(
            boolean lastActionOk,
            boolean userAction,
            List<PlayerShopOfferSnapshot> previousOffers,
            int previousReturns
    ) {
        if (selected >= menu.offers().size()) {
            selected = -1;
        }
        offerPage = Math.min(offerPage, PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize));
        boolean editorFocused = editorFocused();
        rememberEditorFocus();
        if (TraderEditorSync.showActionStatus(userAction)) {
            status = Component.translatable(
                    lastActionOk
                            ? "gui.cointcore.player_trader.manage.saved"
                            : "gui.cointcore.player_trader.manage.invalid"
            );
            statusColor = lastActionOk ? 0x2E7D32 : 0xAA0000;
        }
        if (TraderEditorSync.applyServerValuesToEditor(userAction, lastActionOk, editorFocused)) {
            applyDraftFromSelection();
            deleteArmed = false;
            restoreFocus = -1;
        } else {
            captureDraftsFromBoxes();
        }
        boolean pagerChanged = lastOfferPagerMax
                != PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize);
        boolean idsChanged = !TraderViewSync.sameOfferIds(previousOffers, menu.offers());
        if (pagerChanged || idsChanged) {
            rebuildWidgets();
            return;
        }
        if (claimButton != null && previousReturns != menu.returnCount()) {
            claimButton.setMessage(Component.translatable(
                    "gui.cointcore.player_trader.manage.claim",
                    menu.returnCount()
            ));
            claimButton.active = menu.returnCount() > 0;
        }
        refreshRecommendPlacement();
        applyDraftsToBoxes();
    }

    @Override
    protected void init() {
        captureDraftsFromBoxes();
        rememberEditorFocus();
        geom = PlayerTraderManageLayout.fit(width, height);
        imageWidth = geom.guiWidth;
        imageHeight = geom.guiHeight;
        inventoryLabelX = PlayerTraderManageLayout.PAD;
        inventoryLabelY = geom.playerInvLabelY;
        titleLabelY = geom.titleY;
        if (!menu.slots.isEmpty()) {
            GuiSlotMover.move(menu.slots.getFirst(), geom.ghostX, geom.ghostY);
        }
        GuiSlotMover.movePlayerInventory(menu.slots, 1, geom.playerSlotX(0), geom.playerInvY);
        super.init();
        leftPos = ScaledGuiLayout.origin(width, imageWidth, 0, 0, ScaledGuiLayout.MARGIN);
        topPos = ScaledGuiLayout.origin(height, imageHeight, 0, 0, ScaledGuiLayout.MARGIN);
        int shopTabW = PlayerTraderManageLayout.tabWidth(
                font.width(Component.translatable("gui.cointcore.player_trader.tab.shop"))
        );
        int manageTabW = PlayerTraderManageLayout.tabWidth(
                font.width(Component.translatable("gui.cointcore.player_trader.tab.mine"))
        );
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.tab.shop"),
                button -> {
                    GuiCursor.remember();
                    PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, false));
                }
        ).bounds(leftPos + PlayerTraderManageLayout.PAD, topPos + geom.tabY, shopTabW, geom.tabH).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.tab.mine"),
                button -> {
                }
        ).bounds(
                leftPos + PlayerTraderManageLayout.PAD + shopTabW + 4,
                topPos + geom.tabY,
                manageTabW,
                geom.tabH
        ).build()).active = false;

        countBox = field(geom.fieldBox(0), countText, "gui.cointcore.player_trader.manage.count.tooltip", text -> countText = text);
        dealsBox = field(geom.fieldBox(1), dealsText, "gui.cointcore.player_trader.manage.deals.tooltip", text -> dealsText = text);
        dealsBox.setMaxLength(18);
        long recommended = recommendedUnitPrice();
        Component fill = Component.translatable("gui.cointcore.player_trader.manage.recommend.fill");
        recommendFull = recommended > 0L
                ? Component.translatable(
                        "gui.cointcore.player_trader.manage.recommend",
                        Long.toString(recommended)
                )
                : Component.translatable("gui.cointcore.player_trader.manage.recommend.none");
        recommendPlacement = PlayerTraderManageLayout.recommendPlacement(
                geom,
                font.width(fill),
                font.width(recommendFull)
        );
        recommendTruncated = recommendPlacement.truncated();
        priceBox = field(
                recommendPlacement.price(),
                priceText,
                "gui.cointcore.player_trader.manage.buy.tooltip",
                text -> priceText = text
        );
        PlayerTraderManageLayout.Rect fillRect = recommendPlacement.fill();
        fillButton = addRenderableWidget(Button.builder(fill, button -> fillRecommended())
                .bounds(leftPos + fillRect.x(), topPos + fillRect.y(), fillRect.w(), fillRect.h())
                .tooltip(Tooltip.create(recommendFull))
                .build());
        fillButton.active = recommended > 0L;

        Component listLabel = Component.translatable(
                selected >= 0 ? "gui.cointcore.player_trader.manage.save" : "gui.cointcore.player_trader.manage.list"
        );
        Component newLabel = Component.translatable("gui.cointcore.player_trader.manage.new");
        Component cancelLabel = deleteArmed
                ? Component.translatable("gui.cointcore.player_trader.manage.delete_confirm")
                : Component.translatable("gui.cointcore.player_trader.manage.cancel");
        Component claimLabel = Component.translatable("gui.cointcore.player_trader.manage.claim", menu.returnCount());
        PlayerTraderManageLayout.Rect[] actions = selected >= 0
                ? geom.actionButtons(
                        font.width(listLabel),
                        font.width(newLabel),
                        font.width(cancelLabel),
                        font.width(claimLabel)
                )
                : geom.actionButtons(
                        font.width(listLabel),
                        font.width(cancelLabel),
                        font.width(claimLabel)
                );
        addRenderableWidget(Button.builder(listLabel, button -> save())
                .bounds(leftPos + actions[0].x(), topPos + actions[0].y(), actions[0].w(), actions[0].h())
                .build());
        int actionIndex = 1;
        if (selected >= 0) {
            addRenderableWidget(Button.builder(newLabel, button -> clearSelection())
                    .bounds(leftPos + actions[actionIndex].x(), topPos + actions[actionIndex].y(), actions[actionIndex].w(), actions[actionIndex].h())
                    .build());
            actionIndex++;
        }
        deleteButton = addRenderableWidget(Button.builder(cancelLabel, button -> delete())
                .bounds(leftPos + actions[actionIndex].x(), topPos + actions[actionIndex].y(), actions[actionIndex].w(), actions[actionIndex].h())
                .build());
        deleteButton.active = selected >= 0;
        actionIndex++;
        claimButton = addRenderableWidget(Button.builder(claimLabel, button -> claim())
                .bounds(leftPos + actions[actionIndex].x(), topPos + actions[actionIndex].y(), actions[actionIndex].w(), actions[actionIndex].h())
                .build());
        claimButton.active = menu.returnCount() > 0;

        int maxOfferPage = PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize);
        PlayerTraderManageLayout.Rect prev = geom.pagerPrev();
        PlayerTraderManageLayout.Rect next = geom.pagerNext();
        if (PagerChrome.showPrev(offerPage, maxOfferPage)) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> changeOfferPage(-1))
                    .bounds(leftPos + prev.x(), topPos + prev.y(), prev.w(), prev.h())
                    .build());
        }
        if (PagerChrome.showNext(offerPage, maxOfferPage)) {
            addRenderableWidget(Button.builder(Component.literal(">"), button -> changeOfferPage(1))
                    .bounds(leftPos + next.x(), topPos + next.y(), next.w(), next.h())
                    .build());
        }
        lastOfferPagerMax = PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize);
        GuiCursor.restoreIfNeeded();
        restoreEditorFocus();
    }

    private EditBox field(
            PlayerTraderManageLayout.Rect rect,
            String value,
            String tooltipKey,
            Consumer<String> onChange
    ) {
        EditBox box = GuiEditBoxes.create(
                font,
                leftPos + rect.x(),
                topPos + rect.y(),
                rect.w(),
                rect.h(),
                Component.empty(),
                value,
                onChange
        );
        box.setMaxLength(12);
        box.setFilter(text -> text.chars().allMatch(Character::isDigit) || text.isEmpty());
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOfferList(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleMax = geom.guiWidth - PlayerTraderManageLayout.PAD - titleLabelX;
        graphics.drawString(
                font,
                font.width(title) <= titleMax ? title : Component.literal(font.plainSubstrByWidth(title.getString(), titleMax)),
                titleLabelX,
                titleLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        Component revenue = Component.translatable(
                "gui.cointcore.player_trader.revenue",
                menu.lifetimeRevenue()
        );
        graphics.drawString(font, revenue, PlayerTraderManageLayout.PAD, geom.revenueY, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, PlayerTraderManageLayout.PAD + font.width(revenue) + 2, geom.revenueY - 1);
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.player_trader.manage.offers"),
                PlayerTraderManageLayout.PAD,
                geom.sectionLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.player_trader.manage.editor"),
                geom.rightInnerX,
                geom.sectionLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("container.inventory"),
                inventoryLabelX,
                inventoryLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        Component sampleLabel = Component.translatable("gui.cointcore.player_trader.manage.sample");
        int sampleMax = geom.guiWidth - PlayerTraderManageLayout.PAD - geom.sampleLabelX;
        graphics.drawString(
                font,
                font.width(sampleLabel) <= sampleMax
                        ? sampleLabel
                        : Component.literal(font.plainSubstrByWidth(sampleLabel.getString(), sampleMax)),
                geom.sampleLabelX,
                geom.ghostY + 5,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        drawFieldLabel(graphics, "gui.cointcore.player_trader.manage.count", 0);
        drawFieldLabel(graphics, "gui.cointcore.player_trader.manage.deals", 1);
        drawFieldLabel(graphics, "gui.cointcore.player_trader.manage.buy", 2);
        if (recommendPlacement != null && recommendPlacement.hasText()) {
            String rec = recommendFull.getString();
            int max = recommendPlacement.textMaxW();
            graphics.drawString(
                    font,
                    font.width(recommendFull) <= max
                            ? recommendFull
                            : Component.literal(font.plainSubstrByWidth(rec, max)),
                    recommendPlacement.textX(),
                    recommendPlacement.textY(),
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
        }
        if (!status.getString().isEmpty()) {
            graphics.drawString(
                    font,
                    Component.literal(font.plainSubstrByWidth(status.getString(), geom.rightInnerW)),
                    geom.rightInnerX,
                    geom.titleY,
                    statusColor,
                    false
            );
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, String key, int index) {
        Component label = Component.translatable(key);
        int y = geom.fieldLabelY(index);
        int max = geom.rightInnerW;
        graphics.drawString(
                font,
                font.width(label) <= max ? label : Component.literal(font.plainSubstrByWidth(label.getString(), max)),
                geom.rightInnerX,
                y,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaContainerSkin.blitPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaContainerSkin.blitMenuSlots(graphics, leftPos, topPos, menu.slots);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (recommendTruncated && hoveredRecommendText(mouseX, mouseY) && !recommendFull.getString().isEmpty()) {
            graphics.renderTooltip(font, recommendFull, mouseX, mouseY);
            return;
        }
        if (hoveredGhost(mouseX, mouseY)) {
            ItemStack ghost = menu.ghostItem();
            if (!ghost.isEmpty()) {
                graphics.renderTooltip(font, ghost, mouseX, mouseY);
                return;
            }
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.cointcore.player_trader.manage.sample.tooltip"),
                    mouseX,
                    mouseY
            );
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TraderGuiInput.handleKeyPressed(this, keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (TraderGuiInput.handleCharTyped(this, codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getFocused() instanceof EditBox && !(getChildAt(mouseX, mouseY).orElse(null) instanceof EditBox)) {
            setFocused(null);
        }
        int hit = hoveredOffer(mouseX, mouseY);
        if (hit >= 0) {
            select(hit);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos
                && mouseX < leftPos + geom.leftWidth
                && mouseY >= topPos + geom.listY
                && mouseY < topPos + geom.listBottom) {
            if (scrollY > 0) {
                changeOfferPage(-1);
                return true;
            }
            if (scrollY < 0) {
                changeOfferPage(1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderOfferList(GuiGraphics graphics, int mouseX, int mouseY) {
        int start = offerPage * geom.listPageSize;
        for (int row = 0; row < geom.listPageSize; row++) {
            int index = start + row;
            if (index >= menu.offers().size()) {
                break;
            }
            int x = leftPos + geom.listRowX();
            int y = topPos + geom.listRowY(row);
            int w = geom.listRowW();
            if (index == selected) {
                graphics.fill(x, y, x + w, y + geom.listRowH, PlayerTraderManageLayout.HIGHLIGHT);
            } else {
                VanillaContainerSkin.blitOfferRow(graphics, x, y, w, geom.listRowH);
            }
            VanillaContainerSkin.blitSlot(graphics, x, y);
            PlayerShopOfferSnapshot offer = menu.offers().get(index);
            graphics.renderItem(offer.template(), x + 1, y + 1);
            graphics.renderItemDecorations(font, offer.template().copyWithCount(offer.count()), x + 1, y + 1);
            int textX = x + PlayerTraderManageLayout.SLOT + 2;
            int textY = y + 5;
            Component prices = Component.translatable(
                    "gui.cointcore.player_trader.manage.list_prices",
                    Long.toString(offer.stockItems()),
                    Long.toString(GlobalMarketPriceMath.unitPrice(offer.buyPrice(), offer.count())),
                    Integer.toString(offer.count())
            );
            int maxText = x + w - textX - GluonGuiIcon.SIZE - 2;
            graphics.drawString(
                    font,
                    Component.literal(font.plainSubstrByWidth(prices.getString(), Math.max(8, maxText))),
                    textX,
                    textY,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            GluonGuiIcon.blit(graphics, textX + Math.min(font.width(prices), Math.max(8, maxText)) + 1, textY - 1);
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + geom.listRowH) {
                graphics.renderTooltip(font, offer.template(), (int) mouseX, (int) mouseY);
            }
        }
    }

    private int hoveredOffer(double mouseX, double mouseY) {
        int start = offerPage * geom.listPageSize;
        for (int row = 0; row < geom.listPageSize; row++) {
            int index = start + row;
            if (index >= menu.offers().size()) {
                break;
            }
            int x = leftPos + geom.listRowX();
            int y = topPos + geom.listRowY(row);
            if (mouseX >= x && mouseX < x + geom.listRowW()
                    && mouseY >= y && mouseY < y + geom.listRowH) {
                return index;
            }
        }
        return -1;
    }

    public Rect2i ghostDropArea() {
        return new Rect2i(leftPos + geom.ghostX, topPos + geom.ghostY, 18, 18);
    }

    public void acceptGhostIngredient(ItemStack stack) {
        applyGhostTemplate(stack);
        rebuildKeepingInput();
    }

    private void applyGhostTemplate(ItemStack stack) {
        ItemStack unit = stack == null || stack.isEmpty() ? ItemStack.EMPTY : GhostTemplate.sanitize(stack);
        if (stack != null && !stack.isEmpty() && unit.isEmpty()) {
            return;
        }
        menu.setGhost(unit);
        PacketDistributor.sendToServer(new PlayerTraderGhostPayload(menu.containerId, unit));
    }

    private boolean hoveredRecommendText(int mouseX, int mouseY) {
        if (recommendPlacement == null || !recommendPlacement.hasText()) {
            return false;
        }
        int x = leftPos + recommendPlacement.textX();
        int y = topPos + recommendPlacement.textY();
        return mouseX >= x
                && mouseX < x + recommendPlacement.textMaxW()
                && mouseY >= y
                && mouseY < y + PlayerTraderManageLayout.LABEL_H;
    }

    private boolean hoveredGhost(int mouseX, int mouseY) {
        int x = leftPos + geom.ghostX;
        int y = topPos + geom.ghostY;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void select(int index) {
        selected = index;
        deleteArmed = false;
        PlayerShopOfferSnapshot offer = menu.offers().get(index);
        applyGhostTemplate(offer.template());
        applyDraftFromSelection();
        restoreFocus = -1;
        rebuildWidgets();
    }

    private void fillRecommended() {
        long unit = recommendedUnitPrice();
        if (unit <= 0L) {
            return;
        }
        priceText = Long.toString(unit);
        GuiEditBoxes.write(priceBox, priceText);
    }

    private long recommendedUnitPrice() {
        ItemStack stack = menu.ghostItem();
        if (stack.isEmpty() && selected >= 0 && selected < menu.offers().size()) {
            stack = menu.offers().get(selected).template();
        }
        return menu.recommendedUnitPrice(stack);
    }

    private void save() {
        deleteArmed = false;
        int count = parseInt(countBox.getValue(), 1);
        long deals = parseLong(dealsBox.getValue());
        if (deals < 1L) {
            deals = 1L;
        }
        long price = parseLong(priceBox.getValue());
        ItemStack template = GhostTemplate.sanitize(menu.ghostItem());
        PacketDistributor.sendToServer(new PlayerTraderManagePayload(
                menu.containerId,
                PlayerTraderManagePayload.Action.SAVE,
                selected,
                template,
                count,
                deals,
                price,
                0L
        ));
    }

    private void delete() {
        if (selected < 0) {
            return;
        }
        if (!deleteArmed) {
            deleteArmed = true;
            rebuildKeepingInput();
            return;
        }
        PacketDistributor.sendToServer(new PlayerTraderManagePayload(
                menu.containerId,
                PlayerTraderManagePayload.Action.DELETE,
                selected,
                ItemStack.EMPTY,
                1,
                1,
                0L,
                0L
        ));
        selected = -1;
        deleteArmed = false;
        resetDraftsForNewLot();
        restoreFocus = -1;
        rebuildWidgets();
    }

    private void claim() {
        PacketDistributor.sendToServer(new PlayerTraderManagePayload(
                menu.containerId,
                PlayerTraderManagePayload.Action.CLAIM,
                -1,
                ItemStack.EMPTY,
                1,
                1,
                0L,
                0L
        ));
    }

    private void clearSelection() {
        selected = -1;
        deleteArmed = false;
        applyGhostTemplate(ItemStack.EMPTY);
        resetDraftsForNewLot();
        restoreFocus = -1;
        rebuildWidgets();
    }

    private void changeOfferPage(int delta) {
        offerPage = Math.max(
                0,
                Math.min(PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize), offerPage + delta)
        );
        rebuildKeepingInput();
    }

    private void rebuildKeepingInput() {
        rememberEditorFocus();
        captureDraftsFromBoxes();
        rebuildWidgets();
        restoreEditorFocus();
    }

    private void applyDraftsToBoxes() {
        writeBox(countBox, countText);
        writeBox(dealsBox, dealsText);
        writeBox(priceBox, priceText);
    }

    private void writeBox(EditBox box, String text) {
        GuiEditBoxes.writeUnfocused(box, text);
    }

    private void refreshRecommendPlacement() {
        long recommended = recommendedUnitPrice();
        recommendFull = recommended > 0L
                ? Component.translatable(
                        "gui.cointcore.player_trader.manage.recommend",
                        Long.toString(recommended)
                )
                : Component.translatable("gui.cointcore.player_trader.manage.recommend.none");
        if (fillButton != null) {
            fillButton.active = recommended > 0L;
            fillButton.setTooltip(Tooltip.create(recommendFull));
        }
    }

    private void captureDraftsFromBoxes() {
        if (countBox != null) {
            countText = countBox.getValue();
        }
        if (dealsBox != null) {
            dealsText = dealsBox.getValue();
        }
        if (priceBox != null) {
            priceText = priceBox.getValue();
        }
    }

    private void applyDraftFromSelection() {
        if (selected < 0 || selected >= menu.offers().size()) {
            resetDraftsForNewLot();
            return;
        }
        PlayerShopOfferSnapshot offer = menu.offers().get(selected);
        countText = Integer.toString(offer.count());
        dealsText = "0";
        priceText = Long.toString(Math.max(1L, GlobalMarketPriceMath.unitPrice(offer.buyPrice(), offer.count())));
    }

    private void resetDraftsForNewLot() {
        countText = "1";
        dealsText = "1";
        priceText = "1";
    }

    private boolean editorFocused() {
        return getFocused() instanceof EditBox;
    }

    private void rememberEditorFocus() {
        restoreFocus = -1;
        restoreCursor = -1;
        EditBox focused = focusedEditor();
        if (focused == null) {
            return;
        }
        restoreCursor = focused.getCursorPosition();
        if (focused == countBox) {
            restoreFocus = 0;
        } else if (focused == dealsBox) {
            restoreFocus = 1;
        } else if (focused == priceBox) {
            restoreFocus = 2;
        }
    }

    private void restoreEditorFocus() {
        EditBox box = switch (restoreFocus) {
            case 0 -> countBox;
            case 1 -> dealsBox;
            case 2 -> priceBox;
            default -> null;
        };
        GuiEditBoxes.restoreFocus(this, box, true, restoreCursor);
    }

    private EditBox focusedEditor() {
        if (getFocused() instanceof EditBox box) {
            return box;
        }
        if (countBox != null && countBox.isFocused()) {
            return countBox;
        }
        if (dealsBox != null && dealsBox.isFocused()) {
            return dealsBox;
        }
        if (priceBox != null && priceBox.isFocused()) {
            return priceBox;
        }
        return null;
    }

    private static int parseInt(String text, int fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
