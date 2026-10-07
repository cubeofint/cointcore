package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * Target player inventory: armor + offhand + main + hotbar, then viewer inventory.
 */
public final class InvSeePlayerMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = 41; // 4 armor + offhand + 27 + 9
    /** inventory.png (166) + chest strip label offset (14) — matches generic_54 player slots. */
    public static final int ARMOR_X = 8;
    public static final int ARMOR_Y = 8;
    public static final int OFFHAND_X = 77;
    public static final int OFFHAND_Y = 62;
    public static final int STORAGE_Y = 84;
    public static final int HOTBAR_Y = 142;
    public static final int VIEWER_INVENTORY_Y = 184;
    private static final int[] ARMOR_SLOTS = {39, 38, 37, 36}; // head -> boots

    private final java.util.UUID targetPlayerId;

    public static InvSeePlayerMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        java.util.UUID targetId = buf.readUUID();
        return new InvSeePlayerMenu(containerId, inventory, null, true, targetId);
    }

    public InvSeePlayerMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(
                containerId,
                inventory,
                session,
                false,
                session != null ? session.target().playerId() : java.util.UUID.randomUUID()
        );
    }

    private InvSeePlayerMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            boolean clientSide,
            java.util.UUID targetPlayerId
    ) {
        super(InvSeeMenus.PLAYER.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.targetPlayerId = targetPlayerId;
        finishServerInit();
    }

    public java.util.UUID targetPlayerId() {
        return targetPlayerId;
    }

    @Override
    protected void buildContentSlots() {
        // armor top→bottom
        for (int i = 0; i < 4; i++) {
            createContentSlot(i, ARMOR_X, ARMOR_Y + i * 18);
        }
        createContentSlot(4, OFFHAND_X, OFFHAND_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                createContentSlot(5 + col + row * 9, SLOT_X[col], STORAGE_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            createContentSlot(32 + col, SLOT_X[col], HOTBAR_Y);
        }
    }

    @Override
    public int viewerInventoryY() {
        return VIEWER_INVENTORY_Y;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null) {
            return;
        }
        Inventory inv = target.getInventory();
        boolean edit = editable();
        for (int i = 0; i < 4; i++) {
            contentSlots[i].bindContainer(inv, ARMOR_SLOTS[i], edit);
        }
        contentSlots[4].bindContainer(inv, 40, edit);
        for (int i = 0; i < 27; i++) {
            contentSlots[5 + i].bindContainer(inv, 9 + i, edit);
        }
        for (int i = 0; i < 9; i++) {
            contentSlots[32 + i].bindContainer(inv, i, edit);
        }
    }
}
