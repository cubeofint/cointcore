package com.mawlee.cointcore.modularbees.client;

import com.glodblock.github.modularbees.network.CMBGenericPacket;
import com.glodblock.github.modularbees.network.MBNetworkHandler;
import com.mawlee.cointcore.modularbees.ImportAmountHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Small dialog for one import hatch config slot: how many of the item the hatch keeps in stock. */
final class ImportAmountScreen extends Screen {
    private final Screen parent;
    private final int slot;
    private final ItemStack item;
    private final int current;
    private final int fallback;
    private EditBox input;
    private Button done;

    ImportAmountScreen(Screen parent, int slot, ItemStack item, int current, int fallback) {
        super(Component.translatable("gui.cointcore.import_amount.title"));
        this.parent = parent;
        this.slot = slot;
        this.item = item.copyWithCount(1);
        this.current = current;
        this.fallback = fallback;
    }

    @Override
    protected void init() {
        int x = width / 2;
        int y = height / 2;
        input = new EditBox(font, x - 60, y - 10, 120, 20, Component.translatable("gui.cointcore.import_amount.title"));
        input.setMaxLength(10);
        input.setFilter(text -> text.chars().allMatch(Character::isDigit));
        input.setValue(Integer.toString(current > 0 ? current : fallback));
        input.setResponder(text -> updateDone());
        addRenderableWidget(input);
        setInitialFocus(input);
        done = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> confirm())
                .bounds(x - 100, y + 18, 64, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cointcore.import_amount.default"), button -> send(0))
                .bounds(x - 32, y + 18, 64, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + 36, y + 18, 64, 20).build());
        updateDone();
    }

    /** 1 .. Integer.MAX_VALUE, or -1 if the text is not a valid amount. */
    private int parsed() {
        String text = input == null ? "" : input.getValue();
        if (text.isEmpty()) return -1;
        try {
            long value = Long.parseLong(text);
            return value >= 1 && value <= Integer.MAX_VALUE ? (int) value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void updateDone() {
        if (done != null) done.active = parsed() > 0;
    }

    private void confirm() {
        int value = parsed();
        if (value > 0) send(value);
    }

    private void send(int amount) {
        MBNetworkHandler.INSTANCE.sendToServer(new CMBGenericPacket(ImportAmountHost.SET_ACTION, slot, amount));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = width / 2;
        int y = height / 2;
        graphics.renderItem(item, x - 70 - 18, y - 8);
        graphics.drawCenteredString(font, title, x, y - 44, 0xFFFFFF);
        graphics.drawCenteredString(font, item.getHoverName(), x, y - 30, 0xA0A0A0);
        graphics.drawCenteredString(font, Component.translatable("gui.cointcore.import_amount.hint",
                ImportAmountClient.format(fallback), ImportAmountClient.format(Integer.MAX_VALUE)), x, y + 46, 0x808080);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
