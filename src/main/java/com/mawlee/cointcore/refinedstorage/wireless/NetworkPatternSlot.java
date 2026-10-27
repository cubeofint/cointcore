package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.support.containermenu.ValidatedSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Synchronized network preview, never a source or destination for inventory clicks. */
public final class NetworkPatternSlot extends ValidatedSlot {
    public NetworkPatternSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y, stack -> false);
    }

    @Override public boolean mayPlace(ItemStack stack) { return false; }
    @Override public boolean mayPickup(Player player) { return false; }
}
