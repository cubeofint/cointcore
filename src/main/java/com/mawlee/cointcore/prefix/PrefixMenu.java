package com.mawlee.cointcore.prefix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Vanilla 9×3 chest used as a prefix picker. No extra {@code MenuType} is registered.
 */
public final class PrefixMenu extends ChestMenu {
    private final ServerPlayer player;
    private final List<String> slotGroups;
    private boolean selecting;

    public PrefixMenu(int containerId, Inventory inventory, Container container, ServerPlayer player, List<String> slotGroups) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
        this.player = player;
        this.slotGroups = List.copyOf(slotGroups);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        if (slotId >= 0 && slotId < 27) {
            handlePrefixSlot(slotId);
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, clicker);
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        if (index < 27) {
            handlePrefixSlot(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return clicker == player;
    }

    private void handlePrefixSlot(int slotId) {
        if (selecting || slotId < 0 || slotId >= slotGroups.size()) {
            return;
        }
        String group = slotGroups.get(slotId);
        selecting = true;
        if (group == null) {
            PrefixSelectionService.selectAuto(player.server, player.getUUID(), decision -> {
                PrefixCommand.notifyDecision(player, decision);
                player.closeContainer();
            });
            return;
        }
        PrefixSelectionService.select(player.server, player.getUUID(), group, decision -> {
            PrefixCommand.notifyDecision(player, decision);
            player.closeContainer();
        });
    }
}
