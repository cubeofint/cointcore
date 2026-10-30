package com.mawlee.cointcore.refinedstorage.wireless;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PinRowsButton extends AbstractButton {
    public PinRowsButton(int x, int y) {
        super(x, y, 70, 12, Component.translatable("gui.cointcore.pins.title"));
        setTooltip(Tooltip.create(Component.translatable("tooltip.cointcore.pins.rows")));
    }
    private void change(int button) {
        var board = PinStorage.board();
        var section = Screen.hasControlDown() ? PinBoard.Section.CRAFTING : PinBoard.Section.MANUAL;
        if (button == 0) board.addRow(section); else board.removeRow(section);
        PinStorage.save();
    }
    @Override public void onPress() { change(0); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (active && visible && isMouseOver(x, y) && (button == 0 || button == 1)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            change(button);
            return true;
        }
        return false;
    }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var board = PinStorage.board();
        String text = Component.translatable("gui.cointcore.pins.count", board.rows(PinBoard.Section.MANUAL), board.rows(PinBoard.Section.CRAFTING)).getString();
        graphics.blitSprite(ResourceLocation.parse(isHoveredOrFocused() ? "minecraft:widget/button_highlighted" : "minecraft:widget/button"), getX(), getY(), width, height);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, font.plainSubstrByWidth(text, width - 4), getX() + width / 2, getY() + 2, 0xffffff);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
