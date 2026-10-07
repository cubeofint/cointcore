package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeNestedMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeNestedScreen extends InvSeeBaseScreen<InvSeeNestedMenu> {
    public InvSeeNestedScreen(InvSeeNestedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + InvSeeNestedMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }
}
