package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.menu.InvSeeCurioSlot;
import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class InvSeeCuriosScreen extends InvSeeBaseScreen<InvSeeCuriosMenu> {
    private static final int COSMETIC_FRAME = 0xE8C86BC8;

    private Button prevButton;
    private Button nextButton;

    public InvSeeCuriosScreen(InvSeeCuriosMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.viewerInventoryY() + 82;
    }

    @Override
    protected void initExtraWidgets() {
        int midY = topPos + 18 + (InvSeeCuriosMenu.ROWS * 18) / 2 - 8;
        prevButton = Button.builder(Component.literal("<"), b -> sendButton(InvSeeCuriosMenu.BUTTON_PREV_PAGE))
                .bounds(leftPos - 22, midY, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.prev")))
                .build();
        nextButton = Button.builder(Component.literal(">"), b -> sendButton(InvSeeCuriosMenu.BUTTON_NEXT_PAGE))
                .bounds(leftPos + imageWidth + 4, midY, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cointcore.invsee.page.next")))
                .build();
        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
        updatePageButtons();
    }

    private void updatePageButtons() {
        boolean multi = menu.maxPage() > 0 && !menu.unavailableOffline();
        prevButton.visible = multi;
        nextButton.visible = multi;
        prevButton.active = menu.page() > 0;
        nextButton.active = menu.page() < menu.maxPage();
    }

    @Override
    protected void tickExtraWidgets() {
        updatePageButtons();
    }

    @Override
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.unavailableOffline()) {
            Component unavailable = Component.translatable("gui.cointcore.invsee.curios.offline_unavailable");
            int panelHeight = InvSeeCuriosMenu.ROWS * 18;
            graphics.drawString(
                    font,
                    unavailable,
                    Math.max(8, (imageWidth - font.width(unavailable)) / 2),
                    18 + panelHeight / 2 - 4,
                    VanillaContainerSkin.LABEL_COLOR,
                    false
            );
            return;
        }
        if (menu.maxPage() > 0) {
            Component page = Component.translatable(
                    "gui.cointcore.invsee.page.status",
                    menu.page() + 1,
                    menu.maxPage() + 1
            );
            graphics.drawString(font, page, imageWidth - 8 - font.width(page), 6, VanillaContainerSkin.LABEL_COLOR, false);
        }
    }

    @Override
    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.unavailableOffline()) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (!(slot instanceof InvSeeCurioSlot curio) || !curio.isActive() || !curio.cosmetic()) {
                continue;
            }
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 1, COSMETIC_FRAME);
            graphics.fill(x, y + 17, x + 18, y + 18, COSMETIC_FRAME);
            graphics.fill(x, y, x + 1, y + 18, COSMETIC_FRAME);
            graphics.fill(x + 17, y, x + 18, y + 18, COSMETIC_FRAME);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot instanceof InvSeeCurioSlot curio && curio.hasMeta() && !hoveredSlot.hasItem()) {
            List<Component> lines = new ArrayList<>();
            lines.add(curio.slotTypeName());
            if (curio.cosmetic()) {
                lines.add(Component.translatable("gui.cointcore.invsee.curios.cosmetic_mark"));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot instanceof InvSeeCurioSlot curio && curio.hasMeta()) {
            lines.add(curio.slotTypeLine());
        }
        return lines;
    }
}
