package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.client.VanillaContainerSkin;
import com.mawlee.cointcore.invsee.menu.InvSeePlayerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Target inventory: gray vanilla panel with frames only on real slots, plus entity preview.
 */
public final class InvSeePlayerScreen extends InvSeeBaseScreen<InvSeePlayerMenu> {
    public InvSeePlayerScreen(InvSeePlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = InvSeePlayerMenu.VIEWER_INVENTORY_Y + 82;
    }

    @Override
    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        LivingEntity target = resolveTargetEntity();
        if (target != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    leftPos + 26,
                    topPos + 8,
                    leftPos + 75,
                    topPos + 78,
                    30,
                    0.0625F,
                    mouseX,
                    mouseY,
                    target
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String name = InvSeeClientChrome.displayName();
        if (name == null || name.isBlank()) {
            name = title.getString();
        }
        Component status = Component.translatable(
                InvSeeClientChrome.online() ? "gui.cointcore.invsee.online" : "gui.cointcore.invsee.offline"
        );
        Component line = Component.translatable("gui.cointcore.invsee.target_line", name, status.getString());
        graphics.drawString(
                font,
                font.plainSubstrByWidth(line.getString(), 110),
                8,
                6,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.viewer_inventory"),
                inventoryLabelX,
                inventoryLabelY,
                VanillaContainerSkin.LABEL_COLOR,
                false
        );
    }

    private LivingEntity resolveTargetEntity() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Player online = minecraft.level.getPlayerByUUID(menu.targetPlayerId());
        return online;
    }
}
