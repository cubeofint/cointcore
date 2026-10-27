package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridData;
import net.minecraft.world.entity.player.Inventory;

/** Distinct client menu class: JEI keys transfers by exact class and recipe type, not by MenuType. */
public final class WirelessPatternContainerMenu extends PatternGridContainerMenu {
    public WirelessPatternContainerMenu(int id, Inventory inventory, PatternGridData data) {
        super(id, inventory, data);
    }
}
