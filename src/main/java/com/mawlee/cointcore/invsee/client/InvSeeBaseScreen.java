package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.InvSeeOpenNestedPayload;
import com.mawlee.cointcore.invsee.InvSeeTab;
import com.mawlee.cointcore.invsee.menu.InvSeeBaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Modern InvSee chrome: flat panels, slot wells, copper header — no vanilla GUI textures.
 */
public abstract class InvSeeBaseScreen<T extends InvSeeBaseMenu> extends AbstractContainerScreen<T> {
    private static final Component BRAND = Component.literal("CointCore");

    private InvSeeFlatButton editButton;

    protected InvSeeBaseScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Attachment and other RO sections can hide the mode toggle. */
    protected boolean showEditToggle() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;

        if (showEditToggle()) {
            editButton = new InvSeeFlatButton(
                    leftPos + imageWidth - 54,
                    topPos - InvSeeTheme.HEADER_H - InvSeeTheme.PAD + 3,
                    50,
                    16,
                    editLabel(),
                    button -> sendButton(InvSeeBaseMenu.BUTTON_TOGGLE_EDIT)
            );
            editButton.setTooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.mode.toggle")));
            addRenderableWidget(editButton);
            updateEditButton();
        }
        addTabButtons();
        initExtraWidgets();
    }

    private void addTabButtons() {
        int tabY = topPos - InvSeeTheme.HEADER_H - InvSeeTheme.TAB_H - InvSeeTheme.PAD + 2;
        int x = leftPos - InvSeeTheme.PAD + 4;
        for (InvSeeTab tab : InvSeeClientChrome.tabs()) {
            boolean active = tab.ordinal() == InvSeeClientChrome.activeTab();
            InvSeeFlatButton button = new InvSeeFlatButton(
                    x,
                    tabY,
                    52,
                    14,
                    Component.translatable(tab.langKey()),
                    b -> sendButton(InvSeeBaseMenu.BUTTON_TAB_BASE + tab.ordinal())
            );
            button.style(active ? InvSeeFlatButton.Style.ACCENT : InvSeeFlatButton.Style.NEUTRAL);
            button.setTooltip(Tooltip.create(Component.translatable(tab.langKey())));
            addRenderableWidget(button);
            x += 54;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && hoveredSlot != null && menu.isContentSlot(hoveredSlot.index)) {
            PacketDistributor.sendToServer(new InvSeeOpenNestedPayload(menu.containerId, hoveredSlot.index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Page controls and other section widgets. */
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
        editButton.style(menu.isEditMode() ? InvSeeFlatButton.Style.EDIT : InvSeeFlatButton.Style.ACCENT);
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // ACS normally calls renderBg from here — must keep that chain.
        InvSeeUi.fill(graphics, 0, 0, this.width, this.height, InvSeeTheme.SCRIM);
        this.renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        InvSeeUi.drawShell(
                graphics,
                font,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                BRAND,
                title,
                menu.viewerInventoryY()
        );
        InvSeeUi.drawSlotWells(graphics, leftPos, topPos, menu.slots, mouseX, mouseY);
        renderExtraBg(graphics, partialTick, mouseX, mouseY);
    }

    /** Section-specific overlays (entity preview, etc.). */
    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title lives in the absolute header; only the viewer inventory caption here.
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                InvSeeTheme.MUTED,
                false
        );
        renderExtraLabels(graphics, mouseX, mouseY);
    }

    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    protected InvSeeFlatButton flatButton(int x, int y, int w, int h, Component label, Runnable action) {
        InvSeeFlatButton button = new InvSeeFlatButton(x, y, w, h, label, b -> action.run());
        addRenderableWidget(button);
        return button;
    }
}
