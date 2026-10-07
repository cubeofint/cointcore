package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import lain.mods.cos.impl.inventory.InventoryCosArmor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

/**
 * Stable Cosmetic Armor InvSee: one chest row (4 armor pieces), then viewer inventory.
 * Avoids CosArmor inventory texture / label collisions.
 */
public final class InvSeeCosmeticMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = 4;
    public static final int ROWS = 1;
    public static final int PANEL_TOP = 18;
    /** Visual order: helmet, chestplate, leggings, boots (CosArmor index 3..0). */
    private static final int[] COS_INDEX = {3, 2, 1, 0};

    private final UUID targetId;

    public static InvSeeCosmeticMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        UUID targetId = buf.readUUID();
        return new InvSeeCosmeticMenu(containerId, inventory, null, targetId, true);
    }

    public InvSeeCosmeticMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(containerId, inventory, session, session.target().playerId(), false);
    }

    private InvSeeCosmeticMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            UUID targetId,
            boolean clientSide
    ) {
        super(InvSeeMenus.COSMETIC.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.targetId = targetId;
        finishServerInit();
    }

    public UUID targetId() {
        return targetId;
    }

    @Override
    protected void buildContentSlots() {
        // Center 4 slots in the chest row
        int startX = SLOT_X[2]; // 44
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            createContentSlot(i, startX + i * 18, PANEL_TOP);
        }
    }

    @Override
    public int viewerInventoryY() {
        return 96 + (ROWS - 3) * 18;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null) {
            return;
        }
        CAStacksBase stacks = CosArmorAPI.getCAStacks(target.getUUID());
        if (!(stacks instanceof InventoryCosArmor inventory)) {
            for (InvSeeBoundSlot slot : contentSlots) {
                slot.bindEmpty();
            }
            return;
        }
        boolean edit = editable();
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            int cosIndex = COS_INDEX[i];
            if (cosIndex < inventory.getContainerSize()) {
                contentSlots[i].bindContainer(inventory, cosIndex, edit);
            } else {
                contentSlots[i].bindEmpty();
            }
        }
    }
}
