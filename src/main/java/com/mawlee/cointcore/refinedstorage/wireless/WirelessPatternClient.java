package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridScreen;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.ultramega.universalgrid.common.gui.view.GridTypes;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import com.ultramega.universalgrid.common.interfaces.MixinGridType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class WirelessPatternClient {
    private WirelessPatternClient() {
    }

    private static boolean available() {
        return ModList.get().isLoaded("refinedstorage") && ModList.get().isLoaded("universalgrid");
    }

    public static void renderTabBackgrounds(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        for (var child : screen.children()) {
            if (child instanceof Tab tab) tab.renderBackground(graphics);
        }
    }

    @EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            if (available()) event.register(WirelessPatternIntegration.MENU.get(), PatternGridScreen::new);
        }
    }

    @EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void patternTooltip(ItemTooltipEvent event) {
            if (!available()) return;
            if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                    || !(screen.getSlotUnderMouse() instanceof NetworkPatternSlot slot)
                    || !(screen.getMenu() instanceof WirelessPatternMenu menu)
                    || !ItemStack.isSameItemSameComponents(event.getItemStack(), slot.getItem())) return;
            event.getToolTip().addAll(patternPreviewTooltip(menu.cointcore$availablePatterns()));
        }

        @SubscribeEvent
        public static void patternCount(ScreenEvent.Render.Post event) {
            if (!available()) return;
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                    || !(screen.getMenu() instanceof WirelessPatternMenu menu)
                    || menu.cointcore$patternGridReference() == null) return;
            var graphics = event.getGuiGraphics();
            var font = Minecraft.getInstance().font;
            long count = menu.cointcore$availablePatterns();
            for (var slot : screen.getMenu().slots) {
                if (!(slot instanceof NetworkPatternSlot)) continue;
                if (count > 0 && slot.hasItem()) {
                    String label = count < 10_000 ? Long.toString(count)
                            : NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT).format(count);
                    graphics.pose().pushPose();
                    graphics.pose().translate(screen.getGuiLeft() + slot.x + 17,
                            screen.getGuiTop() + slot.y + 10, 300);
                    float scale = Math.min(1f, 20f / font.width(label));
                    graphics.pose().scale(scale, scale, 1);
                    graphics.drawString(font, label, -font.width(label), 0, 0xffffff, true);
                    graphics.pose().popPose();
                }
                if (!slot.hasItem() && screen.getSlotUnderMouse() == slot) {
                    graphics.renderTooltip(font, patternPreviewTooltip(count), Optional.empty(), event.getMouseX(), event.getMouseY());
                }
            }
        }

        @SubscribeEvent
        public static void init(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                ensureTabs(screen, event::addListener);
            }
        }
    }

    private static List<Component> patternPreviewTooltip(long count) {
        return List.of(
                Component.translatable("tooltip.cointcore.wireless_patterns.ghost").withStyle(ChatFormatting.AQUA),
                Component.translatable("tooltip.cointcore.wireless_patterns.read_only").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.cointcore.wireless_patterns.available", NumberFormat.getIntegerInstance().format(count))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.cointcore.wireless_patterns.consume").withStyle(ChatFormatting.GRAY));
    }

    public static void ensureTabs(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> addWidget) {
        if (!available() || !(screen.getMenu() instanceof AbstractBaseContainerMenu menu)
                || screen.children().stream().anyMatch(Tab.class::isInstance)) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean editor = menu instanceof WirelessPatternMenu wireless && wireless.cointcore$patternGridReference() != null;
        var reference = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
        if (reference == null || reference.resolve(player).filter(WirelessPatternIntegration::isUniversalGrid).isEmpty()) return;
        boolean monitor = !editor && ((MixinGridType) menu).universalgrid$getGridType() == GridTypes.WIRELESS_AUTOCRAFTING_MONITOR;
        int x = screen.getGuiLeft() + screen.getXSize() - (monitor ? 4 : 21);
        int y = screen.getGuiTop() + screen.getYSize() - 78;
        var icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("refinedstorage", "pattern_grid")));
        addWidget.accept(new Tab(x, y, icon, Component.translatable("menu.cointcore.wireless_pattern_grid"), editor, false,
                () -> ((MixinGridType) menu).universalgrid$setGridType(GridTypes.WIRELESS_GRID, player)));
        var crafting = GridTypes.WIRELESS_CRAFTING_GRID;
        addWidget.accept(new Tab(x, y + 26, crafting.getIcon(), crafting.getTooltip(),
                !editor && ((MixinGridType) menu).universalgrid$getGridType() == crafting, false,
                () -> ((MixinGridType) menu).universalgrid$setGridType(crafting, player)));
        var monitoring = GridTypes.WIRELESS_AUTOCRAFTING_MONITOR;
        addWidget.accept(new Tab(x, y + 52, monitoring.getIcon(), monitoring.getTooltip(), monitor, true,
                () -> ((MixinGridType) menu).universalgrid$setGridType(monitoring, player)));
    }

    private static final class Tab extends AbstractButton {
        private final ItemStack icon;
        private final boolean selected;
        private final boolean bottom;
        private final Runnable action;

        private Tab(int x, int y, ItemStack icon, Component title, boolean selected, boolean bottom, Runnable action) {
            super(x, y, 32, 26, title);
            this.icon = icon;
            this.selected = selected;
            this.bottom = bottom;
            this.action = action;
            setTooltip(Tooltip.create(title));
        }

        @Override public void onPress() { if (!selected) action.run(); }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // The widget handles input/tooltips only. Draw its tab behind the container, like Universal Grid.
        }

        private void renderBackground(GuiGraphics graphics) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, selected ? 1 : 0);
            graphics.blitSprite(ResourceLocation.fromNamespaceAndPath("universalgrid",
                            selected ? (bottom ? "selected_tab_bottom" : "selected_tab") : "unselected_tab"),
                    getX(), getY(), width, height);
            graphics.renderItem(icon, getX() + 9, getY() + 5);
            graphics.pose().popPose();
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
