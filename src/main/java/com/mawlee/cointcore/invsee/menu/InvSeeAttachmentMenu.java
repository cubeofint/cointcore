package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeAttachmentItems;
import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class InvSeeAttachmentMenu extends InvSeeBaseMenu {
    public static final int CONTENT_SLOTS = 54;
    public static final int ROWS = 6;

    private final String attachmentKey;

    public static InvSeeAttachmentMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        String key = buf.readUtf();
        return new InvSeeAttachmentMenu(containerId, inventory, null, key, true);
    }

    public InvSeeAttachmentMenu(int containerId, Inventory inventory, InvSeeSession session, String attachmentKey) {
        this(containerId, inventory, session, attachmentKey, false);
    }

    private InvSeeAttachmentMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            String attachmentKey,
            boolean clientSide
    ) {
        super(InvSeeMenus.ATTACHMENT.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.attachmentKey = attachmentKey;
        finishServerInit();
    }

    public String attachmentKey() {
        return attachmentKey;
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
        return 152;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null || viewer == null) {
            return;
        }
        HolderLookup.Provider registries = viewer.registryAccess();
        CompoundTag attachments = target.serializeAttachments(registries);
        CompoundTag data = attachments.contains(attachmentKey)
                ? attachments.getCompound(attachmentKey)
                : new CompoundTag();
        List<ItemStack> items = InvSeeAttachmentItems.extractItems(data, registries);
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            if (i < items.size()) {
                contentSlots[i].bindReadOnly(items.get(i));
            } else {
                contentSlots[i].bindEmpty();
            }
        }
    }
}
