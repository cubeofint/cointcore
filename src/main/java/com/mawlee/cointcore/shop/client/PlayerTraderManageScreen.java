package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.shop.PlayerShopOfferSnapshot;
import com.mawlee.cointcore.shop.PlayerShopValidation;
import com.mawlee.cointcore.shop.PlayerTraderManageMenu;
import com.mawlee.cointcore.shop.PlayerTraderManagePayload;
import com.mawlee.cointcore.shop.PlayerTraderTabPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlayerTraderManageScreen extends AbstractContainerScreen<PlayerTraderManageMenu> {
    private EditBox countBox;
    private EditBox buyBox;
    private EditBox sellBox;
    private int selected = -1;
    private int offerPage;
    private Component status = Component.empty();

    public PlayerTraderManageScreen(PlayerTraderManageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PlayerTraderManageMenu.GUI_WIDTH;
        this.imageHeight = PlayerTraderManageMenu.GUI_HEIGHT;
        this.inventoryLabelY = PlayerTraderManageMenu.PLAYER_INV_Y - 11;
        this.titleLabelY = 18;
    }

    void onOffersUpdated(boolean lastActionOk) {
        status = Component.translatable(
                lastActionOk
                        ? "gui.cointcore.player_trader.manage.saved"
                        : "gui.cointcore.player_trader.manage.invalid"
        );
        if (selected >= menu.offers().size()) {
            selected = -1;
        }
        rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.tab.shop"),
                button -> PacketDistributor.sendToServer(new PlayerTraderTabPayload(menu.containerId, false))
        ).bounds(leftPos + 8, topPos + 4, 70, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.tab.manage"),
                button -> {
                }
        ).bounds(leftPos + 80, topPos + 4, 88, 12).build()).active = false;

        countBox = field(40, PlayerTraderManageMenu.EDITOR_Y, 28, selected >= 0 ? String.valueOf(menu.offers().get(selected).count()) : "1");
        buyBox = field(86, PlayerTraderManageMenu.EDITOR_Y, 40, selected >= 0 ? String.valueOf(menu.offers().get(selected).buyPrice()) : "0");
        sellBox = field(86, PlayerTraderManageMenu.EDITOR_Y + 14, 40, selected >= 0 ? String.valueOf(menu.offers().get(selected).sellPrice()) : "0");

        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.manage.new"),
                button -> {
                    selected = -1;
                    menu.setGhost(ItemStack.EMPTY);
                    countBox.setValue("1");
                    buyBox.setValue("0");
                    sellBox.setValue("0");
                }
        ).bounds(leftPos + 130, topPos + PlayerTraderManageMenu.EDITOR_Y, 38, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.manage.save"),
                button -> save()
        ).bounds(leftPos + 130, topPos + PlayerTraderManageMenu.EDITOR_Y + 14, 38, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cointcore.player_trader.manage.delete"),
                button -> delete()
        ).bounds(leftPos + 130, topPos + PlayerTraderManageMenu.EDITOR_Y + 28, 38, 12).build());

        int stripY = topPos + PlayerTraderManageMenu.EDITOR_Y + 32;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changeOfferPage(-1))
                .bounds(leftPos + 8, stripY, 12, 12).build()).active = offerPage > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changeOfferPage(1))
                .bounds(leftPos + 156, stripY, 12, 12).build()).active = offerPage < maxOfferPage();
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 12, Component.empty());
        box.setMaxLength(12);
        box.setValue(value);
        box.setFilter(text -> text.chars().allMatch(Character::isDigit) || text.isEmpty());
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderOfferStrip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VanillaContainerSkin.LABEL_COLOR, false);
        Component revenue = Component.translatable(
                "gui.cointcore.player_trader.revenue",
                menu.lifetimeRevenue()
        );
        graphics.drawString(font, revenue, 8, PlayerTraderManageMenu.STOCK_Y - 10, VanillaContainerSkin.LABEL_COLOR, false);
        GluonGuiIcon.blit(graphics, 8 + font.width(revenue) + 2, PlayerTraderManageMenu.STOCK_Y - 11);
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.player_trader.manage.count"),
                28,
                PlayerTraderManageMenu.EDITOR_Y + 2,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.player_trader.manage.buy"),
                70,
                PlayerTraderManageMenu.EDITOR_Y + 2,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.player_trader.manage.sell"),
                70,
                PlayerTraderManageMenu.EDITOR_Y + 16,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, VanillaContainerSkin.LABEL_COLOR, false);
        if (!status.getString().isEmpty()) {
            graphics.drawString(font, status, 8, PlayerTraderManageMenu.PLAYER_INV_Y - 22, 0x2E7D32, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaContainerSkin.blitPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaContainerSkin.blitMenuSlots(graphics, leftPos, topPos, menu.slots);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int hit = hoveredOffer(mouseX, mouseY);
        if (hit >= 0) {
            select(hit);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderOfferStrip(GuiGraphics graphics, int mouseX, int mouseY) {
        int start = offerPage * 7;
        int y = topPos + PlayerTraderManageMenu.EDITOR_Y + 32;
        for (int col = 0; col < 7; col++) {
            int index = start + col;
            if (index >= menu.offers().size()) {
                break;
            }
            int x = leftPos + 22 + col * 18;
            VanillaContainerSkin.blitSlot(graphics, x, y);
            PlayerShopOfferSnapshot offer = menu.offers().get(index);
            graphics.renderItem(offer.template(), x + 1, y + 1);
            graphics.renderItemDecorations(font, offer.template().copyWithCount(offer.count()), x + 1, y + 1);
            if (index == selected) {
                graphics.fill(x, y, x + 18, y + 1, 0xFFFFFF00);
            }
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                graphics.renderTooltip(font, offer.template(), (int) mouseX, (int) mouseY);
            }
        }
    }

    private int hoveredOffer(double mouseX, double mouseY) {
        int start = offerPage * 7;
        int y = topPos + PlayerTraderManageMenu.EDITOR_Y + 32;
        for (int col = 0; col < 7; col++) {
            int index = start + col;
            if (index >= menu.offers().size()) {
                break;
            }
            int x = leftPos + 22 + col * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                return index;
            }
        }
        return -1;
    }

    private void select(int index) {
        selected = index;
        PlayerShopOfferSnapshot offer = menu.offers().get(index);
        menu.setGhost(offer.template());
        countBox.setValue(Integer.toString(offer.count()));
        buyBox.setValue(Long.toString(offer.buyPrice()));
        sellBox.setValue(Long.toString(offer.sellPrice()));
    }

    private void save() {
        int count = parseInt(countBox.getValue(), 1);
        long buy = parseLong(buyBox.getValue());
        long sell = parseLong(sellBox.getValue());
        ItemStack template = menu.ghostItem();
        if (!PlayerShopValidation.offerValid(template.isEmpty(), count, buy, sell)) {
            status = Component.translatable("gui.cointcore.player_trader.manage.invalid");
            return;
        }
        PacketDistributor.sendToServer(new PlayerTraderManagePayload(
                menu.containerId,
                PlayerTraderManagePayload.Action.SAVE,
                selected,
                template,
                count,
                buy,
                sell
        ));
    }

    private void delete() {
        if (selected < 0) {
            return;
        }
        PacketDistributor.sendToServer(new PlayerTraderManagePayload(
                menu.containerId,
                PlayerTraderManagePayload.Action.DELETE,
                selected,
                ItemStack.EMPTY,
                1,
                0L,
                0L
        ));
        selected = -1;
    }

    private void changeOfferPage(int delta) {
        offerPage = Math.max(0, Math.min(maxOfferPage(), offerPage + delta));
        rebuildWidgets();
    }

    private int maxOfferPage() {
        int size = menu.offers().size();
        if (size <= 7) {
            return 0;
        }
        return (size - 1) / 7;
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
