package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeePlayerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Target player inventory — flat chrome + portrait frame (no inventory.png).
 */
public final class InvSeePlayerScreen extends InvSeeBaseScreen<InvSeePlayerMenu> {
    private static final int TARGET_PANEL_HEIGHT = 166;
    private static final int VIEWER_PANEL_HEIGHT = 96;

    public InvSeePlayerScreen(InvSeePlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = TARGET_PANEL_HEIGHT + VIEWER_PANEL_HEIGHT;
        this.inventoryLabelY = TARGET_PANEL_HEIGHT + 2;
    }

    @Override
    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Portrait well (left of main storage / beside armor)
        InvSeeUi.drawPortraitFrame(graphics, leftPos + 26, topPos + 8, 50, 70);

        LivingEntity target = resolveTargetEntity();
        if (target != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    leftPos + 26,
                    topPos + 8,
                    leftPos + 76,
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
    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.cointcore.invsee.section.armor"), 8, 1, InvSeeTheme.FAINT, false);
        graphics.drawString(font, Component.translatable("gui.cointcore.invsee.section.storage"), 80, 74, InvSeeTheme.FAINT, false);
    }

    private LivingEntity resolveTargetEntity() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Player online = minecraft.level.getPlayerByUUID(menu.targetPlayerId());
        return online;
    }
}
