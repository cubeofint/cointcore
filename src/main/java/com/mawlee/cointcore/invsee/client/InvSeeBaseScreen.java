package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.InvSeeChromeLayout;
import com.mawlee.cointcore.invsee.InvSeeOpenNestedPayload;
import com.mawlee.cointcore.invsee.InvSeeScaleLayout;
import com.mawlee.cointcore.invsee.InvSeeTab;
import com.mawlee.cointcore.invsee.menu.InvSeeBaseMenu;
import com.mawlee.cointcore.shop.client.GuiSlotMover;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Vanilla InvSee chrome: generic_54 / inventory textures, creative icon tabs.
 */
public abstract class InvSeeBaseScreen<T extends InvSeeBaseMenu> extends AbstractContainerScreen<T> {
    private Button editButton;
    private int tabScroll;
    private InvSeeTab hoveredTab;
    private InvSeeScaleLayout.Fit fit;
    private int preferredWidth = -1;
    private int preferredHeight = -1;

    protected InvSeeBaseScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = InvSeeBaseMenu.GUI_WIDTH;
    }

    protected boolean showEditToggle() {
        return true;
    }

    @Override
    protected void init() {
        if (preferredHeight < 0) {
            preferredWidth = imageWidth > 0 ? imageWidth : InvSeeBaseMenu.GUI_WIDTH;
            preferredHeight = imageHeight > 0 ? imageHeight : menu.viewerInventoryY() + 82;
        }
        fit = InvSeeScaleLayout.fit(width, height, preferredWidth, preferredHeight);
        imageWidth = fit.imageWidth();
        imageHeight = fit.imageHeight();
        super.init();
        leftPos = fit.leftPos();
        topPos = fit.topPos();
        int viewerStart = menu.slots.size() - 36;
        if (viewerStart >= 0) {
            GuiSlotMover.movePlayerInventory(
                    menu.slots,
                    viewerStart,
                    InvSeeBaseMenu.SLOT_X[0],
                    menu.viewerInventoryY() + fit.viewerYShift()
            );
        }
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.viewerInventoryY() + fit.viewerYShift() - 12;

        if (showEditToggle()) {
            editButton = Button.builder(editLabel(), button -> sendButton(InvSeeBaseMenu.BUTTON_TOGGLE_EDIT))
                    .bounds(
                            fit.editX(),
                            fit.editY(),
                            InvSeeChromeLayout.EDIT_BUTTON_WIDTH,
                            InvSeeChromeLayout.EDIT_BUTTON_HEIGHT
                    )
                    .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.mode.toggle")))
                    .build();
            addRenderableWidget(editButton);
            updateEditButton();
        }
        addTabScrollButtons();
        initExtraWidgets();
    }

    private void addTabScrollButtons() {
        List<InvSeeTab> tabs = InvSeeClientChrome.tabs();
        int perRow = Math.max(1, imageWidth / VanillaContainerSkin.TAB_SHIFT);
        if (tabs.size() <= perRow * tabRowsAllowed()) {
            return;
        }
        var prevBox = InvSeeScaleLayout.sideButton(leftPos, imageWidth, width, Math.max(2, topPos - 24), false);
        Button prev = Button.builder(Component.literal("<"), button -> {
            tabScroll = Math.max(0, tabScroll - 1);
            rebuildWidgets();
        }).bounds(prevBox.x(), prevBox.y(), prevBox.w(), prevBox.h()).build();
        prev.active = tabScroll > 0;
        addRenderableWidget(prev);
        var nextBox = InvSeeScaleLayout.sideButton(leftPos, imageWidth, width, Math.max(2, topPos - 24), true);
        Button next = Button.builder(Component.literal(">"), button -> {
            tabScroll++;
            rebuildWidgets();
        }).bounds(nextBox.x(), nextBox.y(), nextBox.w(), nextBox.h()).build();
        next.active = tabScroll + perRow < tabs.size();
        addRenderableWidget(next);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            InvSeeTab tab = tabAt(mouseX, mouseY);
            if (tab != null) {
                sendButton(InvSeeBaseMenu.BUTTON_TAB_BASE + tab.ordinal());
                return true;
            }
        }
        if (button == 1 && hoveredSlot != null && menu.isContentSlot(hoveredSlot.index)) {
            PacketDistributor.sendToServer(new InvSeeOpenNestedPayload(menu.containerId, hoveredSlot.index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    void resetHoverAfterTabSwitch(double mouseX, double mouseY) {
        hoveredSlot = null;
        hoveredTab = null;
        mouseMoved(mouseX, mouseY);
    }

    protected void initExtraWidgets() {
    }

    protected void sendButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    protected void updateEditButton() {
        if (editButton == null) {
            return;
        }
        boolean can = menu.canToggleEdit();
        boolean busy = menu.isEditBusy();
        editButton.visible = can || busy;
        editButton.active = can;
        editButton.setMessage(editLabel());
    }

    private Component editLabel() {
        if (menu.isEditBusy()) {
            return Component.translatable("gui.cointcore.invsee.mode.busy");
        }
        if (!menu.canToggleEdit()) {
            return Component.translatable("gui.cointcore.invsee.mode.read_only");
        }
        return Component.translatable(
                menu.isEditMode() ? "gui.cointcore.invsee.mode.edit_short" : "gui.cointcore.invsee.mode.read_short"
        );
    }

    @Override
    protected void containerTick() {
        updateEditButton();
        tickExtraWidgets();
    }

    protected void tickExtraWidgets() {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab != null) {
            graphics.renderTooltip(font, Component.translatable(hoveredTab.langKey()), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderCreativeTabs(graphics);
        VanillaContainerSkin.blitPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaContainerSkin.blitMenuSlots(graphics, leftPos, topPos, menu.slots);
        renderExtraBg(graphics, partialTick, mouseX, mouseY);
    }

    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String clippedTitle = font.plainSubstrByWidth(title.getString(), InvSeeChromeLayout.titleMaxWidth(imageWidth));
        graphics.drawString(font, clippedTitle, titleLabelX, titleLabelY, VanillaContainerSkin.LABEL_COLOR, false);
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.viewer_inventory"),
                inventoryLabelX,
                inventoryLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        renderExtraLabels(graphics, mouseX, mouseY);
    }

    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected Button vanillaButton(int x, int y, int w, int h, Component label, Runnable action) {
        Button button = Button.builder(label, b -> action.run()).bounds(x, y, w, h).build();
        addRenderableWidget(button);
        return button;
    }

    private int tabY(int row) {
        int y = topPos - VanillaContainerSkin.TAB_HEIGHT + 4 - row * (VanillaContainerSkin.TAB_HEIGHT - 4);
        return Math.max(0, y);
    }

    private int tabRowsAllowed() {
        return fit != null && !fit.tabsAbove() ? 1 : 2;
    }

    private void renderCreativeTabs(GuiGraphics graphics) {
        List<InvSeeTab> tabs = visibleTabs();
        int perRow = Math.max(1, imageWidth / VanillaContainerSkin.TAB_SHIFT);
        for (int i = 0; i < tabs.size(); i++) {
            InvSeeTab tab = tabs.get(i);
            int row = i / perRow;
            int col = i % perRow;
            int x = leftPos + col * VanillaContainerSkin.TAB_SHIFT;
            boolean selected = tab.ordinal() == InvSeeClientChrome.activeTab();
            VanillaContainerSkin.blitCreativeTab(graphics, x, tabY(row), selected, InvSeeTabIcons.icon(tab));
        }
    }

    private InvSeeTab tabAt(double mouseX, double mouseY) {
        List<InvSeeTab> tabs = visibleTabs();
        int perRow = Math.max(1, imageWidth / VanillaContainerSkin.TAB_SHIFT);
        for (int i = 0; i < tabs.size(); i++) {
            int row = i / perRow;
            int col = i % perRow;
            int x = leftPos + col * VanillaContainerSkin.TAB_SHIFT;
            int y = tabY(row);
            if (mouseX >= x && mouseX < x + VanillaContainerSkin.TAB_WIDTH
                    && mouseY >= y && mouseY < y + VanillaContainerSkin.TAB_HEIGHT) {
                return tabs.get(i);
            }
        }
        return null;
    }

    private List<InvSeeTab> visibleTabs() {
        List<InvSeeTab> tabs = InvSeeClientChrome.tabs();
        int perRow = Math.max(1, imageWidth / VanillaContainerSkin.TAB_SHIFT);
        if (tabs.size() <= perRow * tabRowsAllowed()) {
            return tabs;
        }
        int start = Math.max(0, Math.min(tabScroll, Math.max(0, tabs.size() - perRow)));
        int end = Math.min(tabs.size(), start + perRow);
        return tabs.subList(start, end);
    }
}
