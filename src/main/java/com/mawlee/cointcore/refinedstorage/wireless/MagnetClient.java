package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MagnetClient {
    private MagnetClient() { }
    private static boolean available() { return ModList.get().isLoaded("refinedstorage") && ModList.get().isLoaded("universalgrid"); }

    @EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
            if (available()) event.register(WirelessMagnet.MENU.get(), MagnetScreen::new);
        }
    }

    @EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            if (!available() || !(event.getScreen() instanceof AbstractGridScreen<?> screen)) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var reference = ((MixinDisabledSlot) screen.getMenu()).universalgrid$getDisabledSlot();
            if (reference == null || reference.resolve(player).filter(WirelessPatternIntegration::isUniversalGrid).isEmpty()) return;
            event.addListener(new MagnetButton(screen.getGuiLeft() - 22, screen.getGuiTop() + screen.getYSize() - 24,
                    screen.getMenu().containerId, reference));
        }

        @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
            if (!available() || !WirelessPatternIntegration.isUniversalGrid(event.getItemStack())
                    || !MagnetState.installed(event.getItemStack())) return;
            event.getToolTip().add(Component.translatable("tooltip.cointcore.magnet.status",
                    Component.translatable(MagnetState.mode(event.getItemStack()).translationKey())));
        }
    }

    private static final class MagnetButton extends AbstractButton {
        private final int menuId;
        private final SlotReference reference;
        private int previousMode = -2;

        MagnetButton(int x, int y, int menuId, SlotReference reference) {
            super(x, y, 20, 20, Component.translatable("menu.cointcore.wireless_magnet"));
            this.menuId = menuId;
            this.reference = reference;
        }

        private ItemStack device() {
            var player = Minecraft.getInstance().player;
            return player == null ? ItemStack.EMPTY : reference.resolve(player).orElse(ItemStack.EMPTY);
        }

        @Override public void onPress() {
            PacketDistributor.sendToServer(new WirelessMagnet.SettingsPayload(menuId,
                    Screen.hasShiftDown() || !MagnetState.installed(device())));
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ItemStack device = device();
            int mode = MagnetState.installed(device) ? MagnetState.mode(device).ordinal() : -1;
            if (previousMode != mode) {
                previousMode = mode;
                setTooltip(Tooltip.create(Component.translatable("tooltip.cointcore.magnet.button",
                        Component.translatable(mode < 0 ? "gui.cointcore.magnet.install" : MagnetMode.fromId(mode).translationKey()))));
            }
            graphics.blitSprite(ResourceLocation.parse(isHoveredOrFocused() ? "minecraft:widget/button_highlighted" : "minecraft:widget/button"),
                    getX(), getY(), width, height);
            graphics.renderItem(new ItemStack(WirelessMagnet.CARD.get()), getX() + 2, getY() + 2);
            if (mode > 0) graphics.fill(getX() + 15, getY() + 2, getX() + 18, getY() + 5, mode == 1 ? 0xff55ff55 : 0xff55ffff);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
