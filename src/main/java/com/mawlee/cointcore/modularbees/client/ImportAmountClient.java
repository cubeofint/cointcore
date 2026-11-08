package com.mawlee.cointcore.modularbees.client;

import com.glodblock.github.modularbees.container.slot.DisplaySlot;
import com.glodblock.github.modularbees.xmod.ae.gui.MEImportGUI;
import com.mawlee.cointcore.modularbees.ImportAmountHost;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Middle click on a config slot of the ME import hatch opens the amount dialog; amounts are drawn on the slots. */
public final class ImportAmountClient {
    private ImportAmountClient() {
    }

    public static void register() {
        // Highest priority and receiveCanceled: mods that use the middle button in inventories (sorting, item pick)
        // must not swallow the click on a hatch config slot.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, true, ImportAmountClient::mouseClicked);
        NeoForge.EVENT_BUS.addListener(ImportAmountClient::renderAmounts);
        NeoForge.EVENT_BUS.addListener(ImportAmountClient::tooltip);
    }

    private static ImportAmountHost host(MEImportGUI gui) {
        return gui.getMenu().getHost() instanceof ImportAmountHost host ? host : null;
    }

    /** The hovered config slot of the hatch, or null. */
    private static DisplaySlot configSlot(MEImportGUI gui, Slot slot) {
        return slot instanceof DisplaySlot display && display.getItemHandler() == gui.getMenu().getHost().getConfig()
                && display.index() < ImportAmountHost.SLOTS ? display : null;
    }

    private static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        boolean middle = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        // Alt + left click is the fallback when the middle button is taken or missing.
        boolean altLeft = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && net.minecraft.client.gui.screens.Screen.hasAltDown();
        if (!(middle || altLeft) || !(event.getScreen() instanceof MEImportGUI gui)) return;
        DisplaySlot slot = configSlot(gui, gui.getSlotUnderMouse());
        ImportAmountHost host = host(gui);
        if (slot == null || host == null || !slot.hasItem()) return;
        ItemStack item = slot.getItem();
        Minecraft.getInstance().setScreen(new ImportAmountScreen(gui, slot.index(), item,
                host.cointcore$amount(slot.index()), defaultTarget(item)));
        event.setCanceled(true);
    }

    private static int defaultTarget(ItemStack item) {
        long value = (long) Math.max(1, item.getMaxStackSize())
                * com.mawlee.cointcore.config.ModularBeesRsConfig.getImportBufferStacks();
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static void renderAmounts(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof MEImportGUI gui)) return;
        ImportAmountHost host = host(gui);
        if (host == null) return;
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        for (Slot raw : gui.getMenu().slots) {
            DisplaySlot slot = configSlot(gui, raw);
            if (slot == null || !slot.hasItem()) continue;
            int custom = host.cointcore$amount(slot.index());
            String text = format(custom > 0 ? custom : defaultTarget(slot.getItem()));
            graphics.pose().pushPose();
            graphics.pose().translate(slot.x + 16, slot.y + 16, 300);
            graphics.pose().scale(0.5F, 0.5F, 1F);
            graphics.drawString(font, text, -font.width(text), -font.lineHeight + 1, custom > 0 ? 0xFFFF55 : 0xFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    private static void tooltip(ItemTooltipEvent event) {
        if (!(Minecraft.getInstance().screen instanceof MEImportGUI gui)) return;
        DisplaySlot slot = configSlot(gui, gui.getSlotUnderMouse());
        ImportAmountHost host = host(gui);
        if (slot == null || host == null || slot.getItem() != event.getItemStack()) return;
        int custom = host.cointcore$amount(slot.index());
        int target = custom > 0 ? custom : defaultTarget(slot.getItem());
        event.getToolTip().add(Component.translatable(custom > 0
                        ? "tooltip.cointcore.import_amount.custom" : "tooltip.cointcore.import_amount.default",
                String.format(Locale.ROOT, "%,d", target)).withStyle(ChatFormatting.YELLOW));
        event.getToolTip().add(Component.translatable("tooltip.cointcore.import_amount.hint").withStyle(ChatFormatting.GRAY));
    }

    /** 4096, 65K, 2.1M, 2.1B. */
    static String format(long value) {
        if (value < 10_000) return Long.toString(value);
        String[] units = {"K", "M", "B"};
        double scaled = value;
        int unit = -1;
        while (scaled >= 1000 && unit < units.length - 1) {
            scaled /= 1000;
            unit++;
        }
        return (scaled < 10 ? String.format(Locale.ROOT, "%.1f", scaled) : Long.toString((long) scaled)) + units[unit];
    }
}
