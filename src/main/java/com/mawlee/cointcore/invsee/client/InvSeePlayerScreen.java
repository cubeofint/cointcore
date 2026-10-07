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
    private static final int VIEWER_PANEL_HEIGHT = 96;

    public InvSeePlayerScreen(InvSeePlayerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = InvSeePlayerMenu.VIEWER_INVENTORY_Y + VIEWER_PANEL_HEIGHT;
    }

    @Override
    protected void renderExtraBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int shift = leftPos + InvSeePlayerMenu.SLOT_ORIGIN - 8;
        InvSeeUi.drawPortraitFrame(graphics, shift + 26, topPos + InvSeePlayerMenu.ARMOR_Y, 50, 70);

        LivingEntity target = resolveTargetEntity();
        if (target != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    shift + 26,
                    topPos + InvSeePlayerMenu.ARMOR_Y,
                    shift + 76,
                    topPos + InvSeePlayerMenu.ARMOR_Y + 70,
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
                font.plainSubstrByWidth(line.getString(), imageWidth - 16),
                8,
                6,
                InvSeeTheme.TEXT,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.armor"),
                InvSeePlayerMenu.SLOT_ORIGIN,
                InvSeePlayerMenu.ARMOR_Y - 10,
                InvSeeTheme.FAINT,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.offhand"),
                InvSeePlayerMenu.SLOT_ORIGIN + 69,
                InvSeePlayerMenu.ARMOR_Y + 3 * 18 - 10,
                InvSeeTheme.FAINT,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.storage"),
                InvSeePlayerMenu.SLOT_ORIGIN,
                InvSeePlayerMenu.STORAGE_Y - 10,
                InvSeeTheme.FAINT,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.cointcore.invsee.section.hotbar"),
                InvSeePlayerMenu.SLOT_ORIGIN,
                InvSeePlayerMenu.HOTBAR_Y - 10,
                InvSeeTheme.FAINT,
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
