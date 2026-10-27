package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MagnetMenu extends AbstractContainerMenu {
    private final SlotReference reference;
    private final ItemStack device;
    private final Player player;
    private int mode;
    private boolean loading = true;
    private final SimpleContainer card = new SimpleContainer(1) {
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return MagnetState.accepts(stack); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() { super.setChanged(); save(); }
    };

    public MagnetMenu(int id, Inventory inventory, SlotReference reference, ItemStack serverDevice) {
        super(WirelessMagnet.MENU.get(), id);
        this.reference = reference;
        device = serverDevice;
        player = inventory.player;
        if (device != null) {
            mode = MagnetState.mode(device).ordinal();
            card.setItem(0, MagnetState.card(device, player.registryAccess()));
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return mode; }
            @Override public void set(int value) { mode = value; }
        });
        addSlot(new Slot(card, 0, 80, 24) {
            @Override public boolean mayPlace(ItemStack stack) { return MagnetState.accepts(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col, 8 + col * 18, 152);
        loading = false;
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return !reference.isDisabledSlot(index); }
            @Override public boolean mayPlace(ItemStack stack) { return !reference.isDisabledSlot(index); }
        });
    }

    public MagnetMode mode() { return MagnetMode.fromId(mode); }
    public boolean hasCard() { return !card.getItem(0).isEmpty(); }

    private void save() {
        if (loading || device == null || !stillValid(player)) return;
        if (!hasCard()) mode = 0;
        MagnetState.save(device, card.getItem(0), mode(), player.registryAccess());
        player.getInventory().setChanged();
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || !stillValid(player) || !hasCard()) return false;
        mode = mode().next().ordinal();
        save();
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return device == null || player.isAlive() && !player.isSpectator() && reference.resolve(player).orElse(ItemStack.EMPTY) == device
                && WirelessPatternIntegration.isUniversalGrid(device);
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (type == ClickType.SWAP && reference.isDisabledSlot(button)) return;
        if (slot >= 0 && slot < slots.size() && slots.get(slot).container == player.getInventory()
                && reference.isDisabledSlot(slots.get(slot).getContainerSlot())) return;
        super.clicked(slot, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack previous = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        save();
        return previous;
    }
}
