package com.mawlee.cointcore.invsee.menu;

import com.mawlee.cointcore.invsee.InvSeeSession;
import com.mawlee.cointcore.invsee.InvSeeSessions;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
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
    public static final int[] SLOT_X = {8, 26, 44, 62, 80, 98, 116, 134, 152};

    protected final InvSeeSession session;
    protected final Player target;
    protected final ServerPlayer viewer;
    protected final boolean clientSide;
    protected final boolean canEdit;
    protected final int contentSlotCount;
    protected final InvSeeBoundSlot[] contentSlots;
    protected final SimpleContainer placeholders;

    private boolean syncedEditMode;
    private boolean syncedCanEdit;
    private final int viewerInvStart;

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
        this.canEdit = viewer != null && PermissionService.has(viewer, CointPermissionNodes.INVSEE_EDIT);
        this.contentSlotCount = contentSlotCount;
        this.placeholders = new SimpleContainer(Math.max(1, contentSlotCount));
        this.contentSlots = new InvSeeBoundSlot[contentSlotCount];

        if (session != null && !canEdit) {
            session.setEditMode(false);
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
                return canEdit ? 1 : 0;
            }

            @Override
            public void set(int value) {
                syncedCanEdit = value != 0;
            }
        });

        if (!clientSide) {
            syncedCanEdit = canEdit;
        }
        // refreshContent() runs from subclass finishServerInit() after its fields exist
    }

    /**
     * Call at the end of every subclass constructor (after field assignment / extra DataSlots).
     * Base {@code super(...)} must not refresh — subclass fields are still uninitialized then.
     */
    protected final void finishServerInit() {
        if (clientSide) {
            return;
        }
        refreshContent();
        syncedEditMode = isEditModeRaw();
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
        return clientSide ? syncedCanEdit : canEdit;
    }

    private boolean isEditModeRaw() {
        return canEdit && session != null && session.isEditMode();
    }

    protected boolean editable() {
        return isEditModeRaw();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || session == null) {
            return false;
        }
        if (id == BUTTON_TOGGLE_EDIT) {
            if (!canEdit) {
                return false;
            }
            session.setEditMode(!session.isEditMode());
            refreshContent();
            broadcastChanges();
            return true;
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
        if (!isEditModeRaw() && isContentSlot(slotId)) {
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (!clientSide && !isEditModeRaw() && isContentSlot(slot.index)) {
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

        if (!isEditModeRaw()) {
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
        return copy;
    }

    protected boolean isContentSlot(int index) {
        return index >= 0 && index < contentSlotCount;
    }

    @Override
    public boolean stillValid(Player player) {
        if (clientSide) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || session == null) {
            return false;
        }
        return !session.isClosed() && PermissionService.has(serverPlayer, CointPermissionNodes.INVSEE);
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
}
