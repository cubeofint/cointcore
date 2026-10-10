package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.ui.ScaledGuiLayout;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Slot.x / Slot.y are {@code final} in 1.21.1; click packets use slot indices, so
 * the client may still reposition them to match a clamped window.
 */
public final class GuiSlotMover {
    private static final Field SLOT_X = field("x");
    private static final Field SLOT_Y = field("y");

    private GuiSlotMover() {
    }

    public static void movePlayerInventory(List<Slot> slots, int firstSlot, int left, int invY) {
        if (slots == null || firstSlot < 0 || firstSlot + 36 > slots.size()) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                move(slots.get(firstSlot + row * 9 + col), left + col * ScaledGuiLayout.SLOT, invY + row * ScaledGuiLayout.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            move(slots.get(firstSlot + 27 + col), left + col * ScaledGuiLayout.SLOT, invY + 58);
        }
    }

    public static void move(Slot slot, int x, int y) {
        if (slot == null) {
            return;
        }
        setInt(SLOT_X, slot, x);
        setInt(SLOT_Y, slot, y);
    }

    public static void shiftFromY(List<Slot> slots, int thresholdY, int dy) {
        if (slots == null || dy == 0) {
            return;
        }
        for (Slot slot : slots) {
            if (slot.y >= thresholdY) {
                setInt(SLOT_Y, slot, slot.y + dy);
            }
        }
    }

    private static Field field(String name) {
        try {
            Field field = Slot.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Slot." + name, exception);
        }
    }

    private static void setInt(Field field, Slot slot, int value) {
        try {
            field.setInt(slot, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
