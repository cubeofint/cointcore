package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeItemContents;
import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeNestedContainer;
import com.mawlee.cointcore.invsee.InvSeeNestedKind;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InvSeeNestedMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = InvSeeItemContents.MAX_SLOTS;
    public static final int ROWS = 6;

    private final ItemStack parent;
    private InvSeeNestedContainer nested;

    public static InvSeeNestedMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        return new InvSeeNestedMenu(containerId, inventory, null, ItemStack.EMPTY, true);
    }

    public InvSeeNestedMenu(int containerId, Inventory inventory, InvSeeSession session, ItemStack parent) {
        this(containerId, inventory, session, parent, false);
    }

    private InvSeeNestedMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            ItemStack parent,
            boolean clientSide
    ) {
        super(InvSeeMenus.NESTED.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.parent = parent == null ? ItemStack.EMPTY : parent;
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
        return 84 + (ROWS - 3) * 18;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || session == null) {
            return;
        }
        InvSeeNestedKind kind = InvSeeItemContents.kind(parent);
        if (!kind.opensMenu() || kind == InvSeeNestedKind.BACKPACK) {
            for (InvSeeBoundSlot slot : contentSlots) {
                slot.bindEmpty();
            }
            return;
        }
        nested = new InvSeeNestedContainer(parent, editable());
        boolean edit = editable();
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            if (i < nested.getContainerSize()) {
                contentSlots[i].bindContainer(nested, i, edit);
            } else {
                contentSlots[i].bindEmpty();
            }
        }
    }
}
