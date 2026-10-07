package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeCosmeticMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeCosmeticScreen extends InvSeeBaseScreen<InvSeeCosmeticMenu> {
    public InvSeeCosmeticScreen(InvSeeCosmeticMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = menu.viewerInventoryY() + 82;
    }
}
