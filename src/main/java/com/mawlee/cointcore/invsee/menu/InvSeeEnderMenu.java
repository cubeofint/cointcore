package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

public final class InvSeeEnderMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = 27;
    public static final int ROWS = 3;

    public static InvSeeEnderMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        return new InvSeeEnderMenu(containerId, inventory, null, true);
    }

    public InvSeeEnderMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(containerId, inventory, session, false);
    }

    private InvSeeEnderMenu(int containerId, Inventory inventory, InvSeeSession session, boolean clientSide) {
        super(InvSeeMenus.ENDER.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        finishServerInit();
    }

    @Override
    protected void buildContentSlots() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                createContentSlot(col + row * 9, SLOT_X[col], 18 + row * 18);
            }
        }
    }

    @Override
    public int viewerInventoryY() {
        return 84;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null) {
            return;
        }
        Container ender = target.getEnderChestInventory();
        boolean edit = editable();
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            contentSlots[i].bindContainer(ender, i, edit);
        }
    }
}
