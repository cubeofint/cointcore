package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeePlayerStacks;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public final class InvSeeBackpackMenu extends InvSeeBaseMenu {
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int CONTENT_SLOTS = 54;
    public static final int ROWS = 6;

    private final String locationKey;
    private int page;
    private int syncedPage;
    private int syncedMaxPage;

    public static InvSeeBackpackMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        String locationKey = buf.readUtf();
        return new InvSeeBackpackMenu(containerId, inventory, null, locationKey, true);
    }

    public InvSeeBackpackMenu(int containerId, Inventory inventory, InvSeeSession session, String locationKey) {
        this(containerId, inventory, session, locationKey, false);
    }

    private InvSeeBackpackMenu(
            int containerId,
            Inventory inventory,
            InvSeeSession session,
            String locationKey,
            boolean clientSide
    ) {
        super(InvSeeMenus.BACKPACK.get(), containerId, inventory, session, CONTENT_SLOTS, clientSide);
        this.locationKey = locationKey;
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

    public String locationKey() {
        return locationKey;
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
        return 140;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null) {
            return;
        }
        InvSeePlayerStacks.LocatedStack located = InvSeePlayerStacks.decodeLocation(target, locationKey);
        if (located == null) {
            for (InvSeeBoundSlot slot : contentSlots) {
                slot.bindEmpty();
            }
            syncedPage = 0;
            syncedMaxPage = 0;
            return;
        }

        InventoryHandler handler = BackpackWrapper.fromStack(located.stack()).getInventoryHandler();
        int total = handler.getSlots();
        int maxPage = total <= 0 ? 0 : Math.max(0, (total - 1) / CONTENT_SLOTS);
        page = Math.min(page, maxPage);
        syncedPage = page;
        syncedMaxPage = maxPage;

        boolean edit = editable();
        int start = page * CONTENT_SLOTS;
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            int slot = start + i;
            if (slot < total) {
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
