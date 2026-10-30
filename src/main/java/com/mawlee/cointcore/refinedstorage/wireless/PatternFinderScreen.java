package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PatternFinderScreen extends Screen {
    private final AbstractGridScreen<?> parent;
    private final GridToolsPackets.Results results;
    private int page;
    public PatternFinderScreen(AbstractGridScreen<?> parent, GridToolsPackets.Results results) {
        super(Component.translatable("gui.cointcore.finder.title", results.hits().size()));
        this.parent = parent;
        this.results = results;
    }
    @Override protected void init() {
        int left = width / 2 - 170, top = height / 2 - 115;
        for (int index = page * 4; index < Math.min(results.hits().size(), page * 4 + 4); index++) {
            final int selected = index;
            var hit = results.hits().get(index);
            int y = top + 29 + (index % 4) * 43;
            addRenderableWidget(Button.builder(Component.translatable("gui.cointcore.finder.mark"), button -> GridToolsClient.highlight(hit.position()))
                    .bounds(left + 240, y, 46, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cointcore.finder.open"), button -> {
                GridToolsClient.request(parent, results.resource(), GridToolsPackets.OPEN, selected);
            }).bounds(left + 289, y, 46, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("←"), button -> { page = Math.max(0, page - 1); rebuildWidgets(); })
                .bounds(left, top + 206, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), button -> { page = Math.min(Math.max(0, (results.hits().size() - 1) / 4), page + 1); rebuildWidgets(); })
                .bounds(left + 45, top + 206, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(left + 235, top + 206, 100, 20).build());
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.renderBackground(graphics, mouseX, mouseY, partial);
        int left = width / 2 - 170, top = height / 2 - 115;
        graphics.fill(left - 8, top - 8, left + 348, top + 234, 0xee20252c);
        graphics.drawCenteredString(font, title, width / 2, top + 3, 0xffffff);
        if (results.hits().isEmpty()) graphics.drawWordWrap(font, Component.translatable("gui.cointcore.finder.empty"), left + 10, top + 40, 310, 0xcccccc);
        for (int index = page * 4; index < Math.min(results.hits().size(), page * 4 + 4); index++) {
            var hit = results.hits().get(index);
            int y = top + 29 + (index % 4) * 43;
            graphics.drawString(font, font.plainSubstrByWidth(hit.name().getString() + " ×" + hit.matches(), 232), left, y, 0xffdc72);
            var position = hit.position();
            graphics.drawString(font, position.pos().getX() + ", " + position.pos().getY() + ", " + position.pos().getZ(), left, y + 12, 0xffffff);
            graphics.drawString(font, font.plainSubstrByWidth(position.dimension().location().toString(), 330), left, y + 24, 0xaaaaaa);
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
