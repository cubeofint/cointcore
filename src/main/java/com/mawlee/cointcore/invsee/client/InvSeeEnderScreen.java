package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.menu.InvSeeEnderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeEnderScreen extends InvSeeBaseScreen<InvSeeEnderMenu> {
    public InvSeeEnderScreen(InvSeeEnderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + InvSeeEnderMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }
}
