package com.mawlee.cointcore.refinedstorage.multicrafter.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.refinedstorage.multicrafter.MultiCrafterMenu;
import com.mawlee.cointcore.refinedstorage.multicrafter.MultiCrafters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** RebornStorage's multiblock crafter window: a page of 13×6 pattern slots per storage, paged with ‹ and ›. */
public final class MultiCrafterScreen extends AbstractContainerScreen<MultiCrafterMenu>
        implements com.refinedmods.refinedstorage.common.api.autocrafting.PatternOutputRenderingScreen {
    // Background sheet from RebornStorage (MIT): a 150×150 panel at 0,0 and a slot frame at 150,0.
    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "textures/gui/multicrafter.png");
    private static final int PANEL = 150;
    private static final int TEXT = 0x404040;

    private Button back;
    private Button next;
    private Button manager;

    public MultiCrafterScreen(MultiCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 240;
        inventoryLabelX = 45;
        inventoryLabelY = 130;
    }

    /** Like the RS autocrafter: patterns in the crafter slots show what they make, the player inventory stays as is. */
    @Override
    public boolean canDisplayOutput(net.minecraft.world.item.ItemStack stack) {
        int patternSlots = MultiCrafterMenu.COLUMNS * MultiCrafterMenu.ROWS;
        for (int i = 0; i < patternSlots; i++) {
            if (menu.slots.get(i).getItem() == stack) return true;
        }
        return false;
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(MultiCrafters.MENU.get(), MultiCrafterScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        Tooltip hint = Tooltip.create(Component.translatable("gui.cointcore.multicrafter.page_hint"));
        back = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(false))
                .bounds(leftPos + 13, topPos + 172, 20, 20).tooltip(hint).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(true))
                .bounds(leftPos + 217, topPos + 172, 20, 20).tooltip(hint).build());
        manager = addRenderableWidget(Button.builder(managerLabel(), button -> click(MultiCrafterMenu.BUTTON_TOGGLE_MANAGER))
                .bounds(leftPos + 140, topPos + 219, 102, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.multicrafter.manager_hint"))).build());
        updateButtons();
    }

    private void turn(boolean forward) {
        click(Screen.hasShiftDown()
                ? (forward ? MultiCrafterMenu.BUTTON_NEXT_10 : MultiCrafterMenu.BUTTON_BACK_10)
                : (forward ? MultiCrafterMenu.BUTTON_NEXT : MultiCrafterMenu.BUTTON_BACK));
    }

    private Component managerLabel() {
        return Component.translatable(menu.isVisibleInManager()
                ? "gui.cointcore.multicrafter.manager_shown" : "gui.cointcore.multicrafter.manager_hidden");
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void updateButtons() {
        if (back == null) return;
        back.active = menu.page() > 0;
        next.active = menu.page() < menu.pageCount() - 1;
        manager.setMessage(managerLabel());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.cointcore.multicrafter.page",
                menu.page() + 1, Math.max(1, menu.pageCount())), 10, 224, TEXT, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int halfWidth = imageWidth / 2;
        int halfHeight = imageHeight / 2;
        // Four corners of the panel stretched to the window size, as RebornStorage's ScreenBuilder does.
        graphics.blit(SHEET, leftPos, topPos, 0, 0, halfWidth, halfHeight);
        graphics.blit(SHEET, leftPos + halfWidth, topPos, PANEL - halfWidth, 0, halfWidth, halfHeight);
        graphics.blit(SHEET, leftPos, topPos + halfHeight, 0, PANEL - halfHeight, halfWidth, halfHeight);
        graphics.blit(SHEET, leftPos + halfWidth, topPos + halfHeight, PANEL - halfWidth, PANEL - halfHeight, halfWidth, halfHeight);
        for (int row = 0; row < MultiCrafterMenu.ROWS; row++) {
            for (int column = 0; column < MultiCrafterMenu.COLUMNS; column++) {
                slot(graphics, 8 + column * 18, 20 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, 44 + column * 18, 140 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(graphics, 44 + column * 18, 198);
        }
    }

    private void slot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SHEET, leftPos + x, topPos + y, PANEL, 0, 18, 18);
    }
}
