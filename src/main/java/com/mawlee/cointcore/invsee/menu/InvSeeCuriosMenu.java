package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fixed chest-like Curios grid (9x5). Pages rebind slots; slot count never changes.
 */
public final class InvSeeCuriosMenu extends InvSeeBaseMenu {
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int PANEL_SLOTS = COLUMNS * ROWS;
    public static final int PANEL_TOP = 18;

    private int page;
    private int syncedPage;
    private int syncedMaxPage;
    /** Parallel lists — avoid nested $ class (some deploys strip *$*.class). */
    private final List<IItemHandler> flatHandlers = new ArrayList<>();
    private final List<Integer> flatSlots = new ArrayList<>();

    public static InvSeeCuriosMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        return new InvSeeCuriosMenu(containerId, inventory, null, true);
    }

    public InvSeeCuriosMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(containerId, inventory, session, false);
    }

    private InvSeeCuriosMenu(int containerId, Inventory inventory, InvSeeSession session, boolean clientSide) {
        super(InvSeeMenus.CURIOS.get(), containerId, inventory, session, PANEL_SLOTS, clientSide);
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

    public int page() {
        return syncedPage;
    }

    public int maxPage() {
        return syncedMaxPage;
    }

    @Override
    protected void buildContentSlots() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = col + row * COLUMNS;
                createContentSlot(index, SLOT_X[col], PANEL_TOP + row * 18);
            }
        }
    }

    @Override
    public int viewerInventoryY() {
        // Same formula as ChestMenu for N rows: 84 + (rows - 3) * 18
        return 84 + (ROWS - 3) * 18;
    }

    @Override
    protected void refreshContent() {
        if (clientSide || target == null) {
            return;
        }
        rebuildFlattened();
        int size = flatHandlers.size();
        int maxPage = size == 0 ? 0 : Math.max(0, (size - 1) / PANEL_SLOTS);
        page = Math.min(page, maxPage);
        syncedPage = page;
        syncedMaxPage = maxPage;

        boolean edit = editable();
        int start = page * PANEL_SLOTS;
        for (int i = 0; i < PANEL_SLOTS; i++) {
            int flatIndex = start + i;
            if (flatIndex < size) {
                contentSlots[i].bindHandler(flatHandlers.get(flatIndex), flatSlots.get(flatIndex), edit);
            } else {
                contentSlots[i].bindEmpty();
            }
        }
    }

    private void rebuildFlattened() {
        flatHandlers.clear();
        flatSlots.clear();
        CuriosApi.getCuriosInventory(target).ifPresent(curios -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.getCurios().entrySet()) {
                IItemHandler stacks = entry.getValue().getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    flatHandlers.add(stacks);
                    flatSlots.add(slot);
                }
            }
        });
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
