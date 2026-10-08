package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import com.mawlee.cointcore.invsee.InvSeeSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
    private int syncedUnavailable;
    /** Parallel lists — avoid nested $ class (some deploys strip *$*.class). */
    private final List<IItemHandler> flatHandlers = new ArrayList<>();
    private final List<Integer> flatSlots = new ArrayList<>();
    private final List<String> flatIds = new ArrayList<>();
    private final List<Boolean> flatCosmetic = new ArrayList<>();
    private final List<Boolean> flatVisible = new ArrayList<>();

    public static InvSeeCuriosMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBoolean();
        return new InvSeeCuriosMenu(containerId, inventory, null, true);
    }

    public InvSeeCuriosMenu(int containerId, Inventory inventory, InvSeeSession session) {
        this(containerId, inventory, session, false);
    }

    private InvSeeCuriosMenu(int containerId, Inventory inventory, InvSeeSession session, boolean clientSide) {
        super(InvSeeMenus.CURIOS.get(), containerId, inventory, session, PANEL_SLOTS, clientSide);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return syncedPage;
            }

            @Override
            public void set(int value) {
                syncedPage = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return syncedMaxPage;
            }

            @Override
            public void set(int value) {
                syncedMaxPage = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return syncedUnavailable;
            }

            @Override
            public void set(int value) {
                syncedUnavailable = value;
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

    public boolean unavailableOffline() {
        return syncedUnavailable != 0;
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
        return 84 + (ROWS - 3) * 18;
    }

    @Override
    protected void refreshContent() {
        if (clientSide) {
            return;
        }
        if (target == null) {
            syncedUnavailable = session != null && session.target().isOffline() ? 1 : 0;
            clearBoundSlots();
            return;
        }
        rebuildFlattened();
        if (flatHandlers.isEmpty() && session != null && session.target().isOffline()) {
            syncedUnavailable = 1;
            clearBoundSlots();
            return;
        }
        syncedUnavailable = 0;
        int size = flatHandlers.size();
        int maxPage = size == 0 ? 0 : Math.max(0, (size - 1) / PANEL_SLOTS);
        page = Math.min(page, maxPage);
        syncedPage = page;
        syncedMaxPage = maxPage;

        boolean edit = editable();
        int start = page * PANEL_SLOTS;
        int bound = 0;
        for (int i = 0; i < PANEL_SLOTS; i++) {
            int flatIndex = start + i;
            if (flatIndex < size) {
                contentSlots[i].bindHandler(
                        flatHandlers.get(flatIndex),
                        flatSlots.get(flatIndex),
                        edit,
                        curioValidator(flatIndex)
                );
                bound++;
            } else {
                contentSlots[i].bindEmpty();
            }
        }
        setBoundSlotCount(bound);
    }

    private void clearBoundSlots() {
        page = 0;
        syncedPage = 0;
        syncedMaxPage = 0;
        for (int i = 0; i < PANEL_SLOTS; i++) {
            contentSlots[i].bindEmpty();
        }
        setBoundSlotCount(0);
    }

    private void rebuildFlattened() {
        flatHandlers.clear();
        flatSlots.clear();
        flatIds.clear();
        flatCosmetic.clear();
        flatVisible.clear();
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(target);
        if (curios.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ICurioStacksHandler> entry : curios.get().getCurios().entrySet()) {
            ICurioStacksHandler handler = entry.getValue();
            addHandler(entry.getKey(), handler, handler.getStacks(), false);
            // Cosmetic stacks always exist in Curios; only show them for slot types that use them.
            if (handler.hasCosmetic()) {
                addHandler(entry.getKey(), handler, handler.getCosmeticStacks(), true);
            }
        }
    }

    private void addHandler(String identifier, ICurioStacksHandler handler, IItemHandler stacks, boolean cosmetic) {
        if (stacks == null) {
            return;
        }
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            flatHandlers.add(stacks);
            flatSlots.add(slot);
            flatIds.add(identifier);
            flatCosmetic.add(cosmetic);
            flatVisible.add(renders(handler, slot));
        }
    }

    private static boolean renders(ICurioStacksHandler handler, int slot) {
        var renders = handler.getRenders();
        return slot < 0 || slot >= renders.size() || renders.get(slot);
    }

    /**
     * Same rule as Curios' own slots: the raw stack handler accepts anything, so edits are
     * validated with CuriosApi.isStackValid (slot tags, ICurioItem#canEquip) on the server.
     */
    private Predicate<ItemStack> curioValidator(int flatIndex) {
        String identifier = flatIds.get(flatIndex);
        int slot = flatSlots.get(flatIndex);
        boolean cosmetic = flatCosmetic.get(flatIndex);
        boolean visible = flatVisible.get(flatIndex);
        return stack -> target != null
                && CuriosApi.isStackValid(new SlotContext(identifier, target, slot, cosmetic, visible), stack);
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
