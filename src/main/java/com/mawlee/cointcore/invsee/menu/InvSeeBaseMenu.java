package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeAuditLog;
import com.mawlee.cointcore.invsee.InvSeeItemStacks;
import com.mawlee.cointcore.invsee.InvSeePermissions;
import com.mawlee.cointcore.invsee.InvSeeSection;
import com.mawlee.cointcore.invsee.InvSeeService;
import com.mawlee.cointcore.invsee.InvSeeSession;
import com.mawlee.cointcore.invsee.InvSeeSessions;
import com.mawlee.cointcore.invsee.InvSeeTab;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared InvSee section menu: fixed content slots + viewer inventory, edit toggle via button.
 */
public abstract class InvSeeBaseMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_EDIT = 100;
    public static final int BUTTON_TAB_BASE = 200;
    public static final int GUI_WIDTH = 176;
    public static final int[] SLOT_X = {8, 26, 44, 62, 80, 98, 116, 134, 152};

    public static final int LOCK_NONE = 0;
    public static final int LOCK_CAN_EDIT = 1;
    public static final int LOCK_BUSY = 2;

    protected final InvSeeSession session;
    protected Player target;
    protected final ServerPlayer viewer;
    protected final boolean clientSide;
    protected final int contentSlotCount;
    protected final InvSeeBoundSlot[] contentSlots;
    protected final SimpleContainer placeholders;

    private boolean syncedEditMode;
    private int syncedLockState;
    private final int viewerInvStart;
    private long seenGeneration = Long.MIN_VALUE;

    protected InvSeeBaseMenu(
            MenuType<?> type,
            int containerId,
            Inventory viewerInventory,
            InvSeeSession session,
            int contentSlotCount,
            boolean clientSide
    ) {
        super(type, containerId);
        this.clientSide = clientSide;
        this.session = session;
        this.target = session != null ? session.target().getPlayer() : null;
        this.viewer = !clientSide ? (ServerPlayer) viewerInventory.player : null;
        this.contentSlotCount = contentSlotCount;
        this.placeholders = new SimpleContainer(Math.max(1, contentSlotCount));
        this.contentSlots = new InvSeeBoundSlot[contentSlotCount];

        if (session != null && viewer != null && !InvSeePermissions.canEdit(viewer, session.section())) {
            session.exitEdit();
        }

        buildContentSlots();
        this.viewerInvStart = slots.size();
        addViewerInventory(viewerInventory, viewerInventoryY());

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return isEditModeRaw() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncedEditMode = value != 0;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return lockState();
            }

            @Override
            public void set(int value) {
                syncedLockState = value;
            }
        });

        if (!clientSide) {
            syncedLockState = lockState();
        }
    }

    protected final void finishServerInit() {
        if (clientSide) {
            return;
        }
        refreshLiveTarget();
        refreshContent();
        syncedEditMode = isEditModeRaw();
        if (session != null) {
            seenGeneration = session.target().generation();
        }
    }

    protected abstract void buildContentSlots();

    protected abstract void refreshContent();

    public abstract int viewerInventoryY();

    protected InvSeeBoundSlot createContentSlot(int index, int x, int y) {
        InvSeeBoundSlot slot = new InvSeeBoundSlot(placeholders, index, x, y, clientSide);
        contentSlots[index] = slot;
        addSlot(slot);
        return slot;
    }

    protected void addViewerInventory(Inventory inventory, int yStart) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, SLOT_X[col], yStart + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, SLOT_X[col], yStart + 58));
        }
    }

    public InvSeeSession session() {
        return session;
    }

    public int contentSlotCount() {
        return contentSlotCount;
    }

    public boolean isEditMode() {
        if (clientSide) {
            return syncedEditMode;
        }
        return isEditModeRaw();
    }

    public boolean canToggleEdit() {
        return lockStateSynced() == LOCK_CAN_EDIT;
    }

    public boolean isEditBusy() {
        return lockStateSynced() == LOCK_BUSY;
    }

    public boolean showEditToggle() {
        return lockStateSynced() != LOCK_NONE;
    }

    private int lockStateSynced() {
        return clientSide ? syncedLockState : lockState();
    }

    private int lockState() {
        if (clientSide || viewer == null || session == null) {
            return syncedLockState;
        }
        if (!InvSeePermissions.canEdit(viewer, session.section()) || session.target().isFrozen()) {
            return LOCK_NONE;
        }
        if (session.target().editLock().isHeld() && !session.target().editLock().isHeldBy(viewer.getUUID())) {
            return LOCK_BUSY;
        }
        return LOCK_CAN_EDIT;
    }

    private boolean isEditModeRaw() {
        return editable();
    }

    protected boolean editable() {
        return viewer != null
                && session != null
                && session.target().isUsable()
                && !session.target().isFrozen()
                && session.isEditMode()
                && InvSeePermissions.canEdit(viewer, session.section());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || session == null || viewer == null) {
            return false;
        }
        if (id == BUTTON_TOGGLE_EDIT) {
            if (!InvSeePermissions.canEdit(viewer, session.section()) || session.target().isFrozen()) {
                session.exitEdit();
                refreshContent();
                broadcastChanges();
                return false;
            }
            if (session.isEditMode()) {
                session.exitEdit();
                InvSeeAuditLog.editMode(viewer.getGameProfile().getName(), session.target().displayName(), false);
            } else if (session.tryEnterEdit(viewer, System.currentTimeMillis())) {
                InvSeeAuditLog.editMode(viewer.getGameProfile().getName(), session.target().displayName(), true);
            } else {
                String editor = session.target().editLock().editorName();
                viewer.sendSystemMessage(CointCoreMessages.forPlayer(
                        viewer,
                        CointCoreMessages.INVSEE_BUSY,
                        editor == null ? "?" : editor
                ));
                return false;
            }
            refreshContent();
            broadcastChanges();
            return true;
        }
        if (id >= BUTTON_TAB_BASE && id < BUTTON_TAB_BASE + InvSeeTab.values().length) {
            InvSeeTab tab = InvSeeTab.fromOrdinalOrInventory(id - BUTTON_TAB_BASE);
            return InvSeeService.openTab(viewer, session.target(), tab);
        }
        return handleSectionButton(player, id);
    }

    protected boolean handleSectionButton(Player player, int id) {
        return false;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clientSide) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }
        if (!mayMutateTarget() && isContentSlot(slotId)) {
            return;
        }
        ItemStack[] before = snapshotContent();
        super.clicked(slotId, dragType, clickType, player);
        logContentChanges(before);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (!clientSide && !mayMutateTarget() && isContentSlot(slot.index)) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (clientSide) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int contentEnd = contentSlotCount;
        int total = slots.size();
        ItemStack[] before = snapshotContent();

        if (!mayMutateTarget()) {
            if (index < contentEnd) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(original, viewerInvStart, total, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < contentEnd) {
            if (!moveItemStackTo(original, viewerInvStart, total, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, contentEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        logContentChanges(before);
        return copy;
    }

    public boolean isContentSlot(int index) {
        return index >= 0 && index < contentSlotCount;
    }

    public ItemStack contentStack(int index) {
        if (!isContentSlot(index)) {
            return ItemStack.EMPTY;
        }
        return contentSlots[index].getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        if (clientSide) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || session == null) {
            return false;
        }
        if (session.isClosed() || !session.target().isUsable()) {
            return false;
        }
        if (!InvSeePermissions.canView(serverPlayer, session.section())) {
            return false;
        }
        return InvSeePermissions.canInspect(serverPlayer, serverPlayer.server, session.target().playerId());
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide && session != null) {
            refreshLiveTarget();
            long generation = session.target().generation();
            if (generation != seenGeneration) {
                seenGeneration = generation;
                refreshContent();
            } else if (session.isEditMode() && !editable()) {
                refreshContent();
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!clientSide
                && session != null
                && player instanceof ServerPlayer serverPlayer
                && serverPlayer.getUUID().equals(session.viewerId())) {
            InvSeeSessions.close(session);
        }
    }

    private boolean mayMutateTarget() {
        return editable();
    }

    private void refreshLiveTarget() {
        if (session != null) {
            target = session.target().getPlayer();
        }
    }

    private ItemStack[] snapshotContent() {
        ItemStack[] items = new ItemStack[contentSlotCount];
        for (int i = 0; i < contentSlotCount; i++) {
            items[i] = contentSlots[i].getItem().copy();
        }
        return items;
    }

    private void logContentChanges(ItemStack[] before) {
        if (viewer == null || session == null || before == null) {
            return;
        }
        boolean changed = false;
        InvSeeSection section = session.section();
        for (int i = 0; i < contentSlotCount; i++) {
            ItemStack after = contentSlots[i].getItem();
            if (InvSeeItemStacks.same(before[i], after)) {
                continue;
            }
            changed = true;
            InvSeeAuditLog.slotChange(
                    viewer.getGameProfile().getName(),
                    session.target().displayName(),
                    section,
                    i,
                    InvSeeItemStacks.describe(before[i]),
                    InvSeeItemStacks.describe(after)
            );
        }
        if (changed) {
            session.target().markDirty();
            session.target().editLock().touch(viewer.getUUID(), System.currentTimeMillis());
        }
    }
}
