package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Placeholder trader UI. No offers or wallet yet — player inventory only.
 */
public class TraderMenu extends AbstractContainerMenu {
    /** One empty chest row above the player inventory (visual placeholder). */
    public static final int CONTAINER_ROWS = 1;

    private final ContainerLevelAccess access;

    public TraderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TraderMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ShopMenus.TRADER.get(), containerId);
        this.access = access;

        int playerInvY = 103 + (CONTAINER_ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
        }
    }

    public static TraderMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new TraderMenu(containerId, playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.TRADER.get());
    }
}
