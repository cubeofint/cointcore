package com.mawlee.cointcore.invsee.menu;

import com.flanks255.pocketstorage.inventory.StorageManager;
import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

public final class InvSeePocketMenu extends InvSeeBaseMenu {
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int CONTENT_SLOTS = 54;
    public static final int ROWS = 6;

    private final UUID storageId;
    private int page;
    private int syncedPage;
    private int syncedMaxPage;
    private int totalSlots;

    public static InvSeePocketMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        UUID storageId = buf.readUUID();
        return new InvSeePocketMenu(containerId, inventory, null, storageId, true);
    }

    public InvSeePocketMenu(int containerId, Inventory inventory, InvSeeSession session, UUID storageId) {
        this(containerId, inventory, session, storageId, false);
    }

    private InvSeePocketMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            UUID storageId,
            boolean clientSide
    ) {
        super(InvSeeMenus.POCKET.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.storageId = storageId;
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return syncedPage;
            }

            @Override
            public void set(int value) {
                syncedPage = value;
            }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return syncedMaxPage;
            }

            @Override
            public void set(int value) {
                syncedMaxPage = value;
            }
        });
        finishServerInit();
    }

    public UUID storageId() {
        return storageId;
    }

    public int page() {
        return syncedPage;
    }

    public int maxPage() {
        return syncedMaxPage;
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
        if (clientSide) {
            return;
        }
        IItemHandler handler = StorageManager.get().getCapability(storageId).orElse(null);
        if (handler == null) {
            for (InvSeeBoundSlot slot : contentSlots) {
                slot.bindEmpty();
            }
            syncedPage = 0;
            syncedMaxPage = 0;
            return;
        }

        totalSlots = handler.getSlots();
        int maxPage = totalSlots <= 0 ? 0 : Math.max(0, (totalSlots - 1) / CONTENT_SLOTS);
        page = Math.min(page, maxPage);
        syncedPage = page;
        syncedMaxPage = maxPage;

        boolean edit = editable();
        int start = page * CONTENT_SLOTS;
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            int slot = start + i;
            if (slot < totalSlots) {
                contentSlots[i].bindHandler(handler, slot, edit);
            } else {
                contentSlots[i].bindEmpty();
            }
        }
    }

    @Override
    protected boolean handleSectionButton(Player player, int id) {
        if (id == BUTTON_PREV_PAGE && page > 0) {
            page--;
            refreshContent();
            broadcastChanges();
            return true;
        }
        if (id == BUTTON_NEXT_PAGE && page < syncedMaxPage) {
            page++;
            refreshContent();
            broadcastChanges();
            return true;
        }
        return false;
    }
}
