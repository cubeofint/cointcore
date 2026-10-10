package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.shop.GhostTemplate;
import com.mawlee.cointcore.shop.GlobalMarketPriceMath;
import com.mawlee.cointcore.shop.PlayerShopOfferSnapshot;
import com.mawlee.cointcore.shop.PlayerTraderGhostPayload;
import com.mawlee.cointcore.shop.PlayerTraderManageLayout;
import com.mawlee.cointcore.shop.PlayerTraderManageMenu;
import com.mawlee.cointcore.shop.PlayerTraderManagePayload;
import com.mawlee.cointcore.shop.PlayerTraderTabPayload;
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
import org.lwjgl.glfw.GLFW;

public class PlayerTraderManageScreen extends AbstractContainerScreen<PlayerTraderManageMenu> {
    private PlayerTraderManageLayout.Geom geom = PlayerTraderManageLayout.fit(
            ScaledGuiLayout.SCALE3_1080P_W,
            ScaledGuiLayout.SCALE3_1080P_H
    );
    private EditBox countBox;
    private EditBox dealsBox;
    private EditBox priceBox;
    private Button deleteButton;
    private int selected = -1;
    private int offerPage;
    private boolean deleteArmed;
    private Component status = Component.empty();
    private int statusColor = 0x2E7D32;

    public PlayerTraderManageScreen(PlayerTraderManageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PlayerTraderManageLayout.GUI_WIDTH;
        this.imageHeight = PlayerTraderManageLayout.GUI_HEIGHT;
        this.inventoryLabelX = PlayerTraderManageLayout.PAD;
        this.inventoryLabelY = PlayerTraderManageLayout.PLAYER_INV_LABEL_Y;
        this.titleLabelY = PlayerTraderManageLayout.TITLE_Y;
    }

    void onOffersUpdated(boolean lastActionOk) {
        status = Component.translatable(
                lastActionOk
                        ? "gui.cointcore.player_trader.manage.saved"
                        : "gui.cointcore.player_trader.manage.invalid"
        );
        statusColor = lastActionOk ? 0x2E7D32 : 0xAA0000;
        if (selected >= menu.offers().size()) {
            selected = -1;
        }
        offerPage = Math.min(offerPage, PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize));
        deleteArmed = false;
        rebuildWidgets();
    }

    @Override
    protected void init() {
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
                button -> PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, false))
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

        countBox = field(geom.fieldBox(0), selected >= 0 ? String.valueOf(menu.offers().get(selected).count()) : "1",
                "gui.cointcore.player_trader.manage.count.tooltip");
        dealsBox = field(geom.fieldBox(1), selected >= 0 ? String.valueOf(menu.offers().get(selected).stockItems()) : "1",
                "gui.cointcore.player_trader.manage.deals.tooltip");
        long recommended = recommendedUnitPrice();
        PlayerTraderManageLayout.Rect priceRect = geom.fieldBox(2);
        Component fill = Component.translatable("gui.cointcore.player_trader.manage.recommend.fill");
        int fillW = recommended > 0L ? Math.max(48, font.width(fill) + 10) : 0;
        boolean inlineRecommend = recommended > 0L && geom.buttonY <= geom.recommendY;
        if (inlineRecommend) {
            priceRect = new PlayerTraderManageLayout.Rect(
                    priceRect.x(),
                    priceRect.y(),
                    Math.max(32, priceRect.w() - fillW - 2),
                    priceRect.h()
            );
        }
        priceBox = field(priceRect, selected >= 0 ? String.valueOf(menu.offers().get(selected).buyPrice()) : "1",
                "gui.cointcore.player_trader.manage.buy.tooltip");
        if (recommended > 0L) {
            int fillX = inlineRecommend ? priceRect.right() + 2 : geom.rightInnerX;
            int fillY = inlineRecommend ? priceRect.y() : geom.recommendY;
            addRenderableWidget(Button.builder(fill, button -> fillRecommended())
                    .bounds(leftPos + fillX, topPos + fillY, fillW, 12)
                    .tooltip(Tooltip.create(Component.translatable(
                            "gui.cointcore.player_trader.manage.recommend",
                            Long.toString(recommended)
                    )))
                    .build());
        }

        Component listLabel = Component.translatable("gui.cointcore.player_trader.manage.list");
        Component cancelLabel = deleteArmed
                ? Component.translatable("gui.cointcore.player_trader.manage.delete_confirm")
                : Component.translatable("gui.cointcore.player_trader.manage.cancel");
        Component claimLabel = Component.translatable("gui.cointcore.player_trader.manage.claim", menu.returnCount());
        PlayerTraderManageLayout.Rect[] actions = geom.actionButtons(
                font.width(listLabel),
                font.width(cancelLabel),
                font.width(claimLabel)
        );
        addRenderableWidget(Button.builder(listLabel, button -> save())
                .bounds(leftPos + actions[0].x(), topPos + actions[0].y(), actions[0].w(), actions[0].h())
                .build());
        deleteButton = addRenderableWidget(Button.builder(cancelLabel, button -> delete())
                .bounds(leftPos + actions[1].x(), topPos + actions[1].y(), actions[1].w(), actions[1].h())
                .build());
        deleteButton.active = selected >= 0;
        addRenderableWidget(Button.builder(claimLabel, button -> claim())
                .bounds(leftPos + actions[2].x(), topPos + actions[2].y(), actions[2].w(), actions[2].h())
                .build()).active = menu.returnCount() > 0;

        PlayerTraderManageLayout.Rect prev = geom.pagerPrev();
        PlayerTraderManageLayout.Rect next = geom.pagerNext();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changeOfferPage(-1))
                .bounds(leftPos + prev.x(), topPos + prev.y(), prev.w(), prev.h())
                .build()).active = offerPage > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changeOfferPage(1))
                .bounds(leftPos + next.x(), topPos + next.y(), next.w(), next.h())
                .build()).active = offerPage < PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize);
    }

    private EditBox field(PlayerTraderManageLayout.Rect rect, String value, String tooltipKey) {
        EditBox box = new EditBox(font, leftPos + rect.x(), topPos + rect.y(), rect.w(), rect.h(), Component.empty());
        box.setMaxLength(12);
        box.setValue(value);
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
        long recommended = recommendedUnitPrice();
        if (recommended > 0L && geom.buttonY > geom.recommendY) {
            Component rec = Component.translatable(
                    "gui.cointcore.player_trader.manage.recommend",
                    Long.toString(recommended)
            );
            graphics.drawString(
                    font,
                    Component.literal(font.plainSubstrByWidth(rec.getString(), geom.rightInnerW)),
                    geom.rightInnerX + 56,
                    geom.recommendY + 2,
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
                    Long.toString(offer.buyPrice()),
                    Integer.toString(offer.stockItems())
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
        countBox.setValue(Integer.toString(offer.count()));
        dealsBox.setValue(Integer.toString(offer.stockItems()));
        priceBox.setValue(Long.toString(offer.buyPrice()));
        rebuildWidgets();
    }

    private void fillRecommended() {
        long unit = recommendedUnitPrice();
        if (unit <= 0L) {
            return;
        }
        int count = parseInt(countBox.getValue(), 1);
        priceBox.setValue(Long.toString(GlobalMarketPriceMath.dealPrice(unit, count)));
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
        int deals = parseInt(dealsBox.getValue(), 1);
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

    private void changeOfferPage(int delta) {
        offerPage = Math.max(
                0,
                Math.min(PlayerTraderManageLayout.maxOfferPage(menu.offers().size(), geom.listPageSize), offerPage + delta)
        );
        rebuildKeepingInput();
    }

    private void rebuildKeepingInput() {
        String count = countBox.getValue();
        String deals = dealsBox.getValue();
        String price = priceBox.getValue();
        rebuildWidgets();
        countBox.setValue(count);
        dealsBox.setValue(deals);
        priceBox.setValue(price);
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
