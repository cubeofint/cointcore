package com.mawlee.cointcore.refinedstorage.multicrafter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

/**
 * The crafter's pattern window, laid out like RebornStorage's: one page per pattern storage (13×6 slots), paged with
 * menu buttons, the player's inventory below.
 */
public final class MultiCrafterMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 13;
    public static final int ROWS = 6;
    static final int SLOTS = COLUMNS * ROWS;
    public static final int BUTTON_BACK = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_BACK_10 = 2;
    public static final int BUTTON_NEXT_10 = 3;
    public static final int BUTTON_TOGGLE_MANAGER = 4;

    private final Level level;
    private final BlockPos origin;
    private final List<BlockPos> storages;
    private final PageContainer page;
    private final DataSlot pageIndex = DataSlot.standalone();
    private final DataSlot pageCount = DataSlot.standalone();
    private final DataSlot visibleInManager = DataSlot.standalone();

    /** Server side: shows the storages of the crafter the player clicked. */
    MultiCrafterMenu(int id, Inventory inventory, BlockPos origin, MultiCrafterShape.Result structure) {
        this(id, inventory, origin, storagesOf(inventory.player.level(), structure), new PageContainer());
        retarget();
        visibleInManager.set(storageEntities().anyMatch(MultiCrafterStorageBlockEntity::isVisibleInManager) ? 1 : 0);
    }

    /** Client side: slots mirror whatever page the server shows. */
    static MultiCrafterMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new MultiCrafterMenu(id, inventory, buffer.readBlockPos(), List.of(), new PageContainer());
    }

    private MultiCrafterMenu(int id, Inventory inventory, BlockPos origin, List<BlockPos> storages, PageContainer page) {
        super(MultiCrafters.MENU.get(), id);
        this.level = inventory.player.level();
        this.origin = origin;
        this.storages = storages;
        this.page = page;
        if (level.isClientSide) {
            // Same filter as the storages so the client predicts placement the way the server decides it.
            page.target = new MultiCrafterStorageBlockEntity.StoragePatternInventory(() -> level);
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new PatternSlot(page, column + row * COLUMNS, 9 + column * 18, 21 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 45 + column * 18, 141 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 45 + column * 18, 199));
        }
        pageCount.set(storages.size());
        addDataSlot(pageIndex);
        addDataSlot(pageCount);
        addDataSlot(visibleInManager);
    }

    private static List<BlockPos> storagesOf(Level level, MultiCrafterShape.Result structure) {
        return structure.parts().stream()
                .map(packed -> new BlockPos(MultiCrafterShape.unpackX(packed), MultiCrafterShape.unpackY(packed),
                        MultiCrafterShape.unpackZ(packed)))
                .filter(pos -> level.getBlockEntity(pos) instanceof MultiCrafterStorageBlockEntity)
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                .toList();
    }

    public int page() {
        return pageIndex.get();
    }

    public int pageCount() {
        return pageCount.get();
    }

    public boolean isVisibleInManager() {
        return visibleInManager.get() != 0;
    }

    private java.util.stream.Stream<MultiCrafterStorageBlockEntity> storageEntities() {
        return storages.stream()
                .map(level::getBlockEntity)
                .filter(MultiCrafterStorageBlockEntity.class::isInstance)
                .map(MultiCrafterStorageBlockEntity.class::cast);
    }

    private void retarget() {
        int index = pageIndex.get();
        page.target = index < storages.size() && level.getBlockEntity(storages.get(index)) instanceof MultiCrafterStorageBlockEntity storage
                ? storage.getPatternContainer() : PageContainer.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_MANAGER) {
            boolean visible = !isVisibleInManager();
            storageEntities().forEach(storage -> storage.setVisibleInManager(visible));
            visibleInManager.set(visible ? 1 : 0);
            return true;
        }
        int delta = switch (id) {
            case BUTTON_BACK -> -1;
            case BUTTON_NEXT -> 1;
            case BUTTON_BACK_10 -> -10;
            case BUTTON_NEXT_10 -> 10;
            default -> 0;
        };
        if (delta == 0 || storages.isEmpty()) return false;
        int next = Math.max(0, Math.min(storages.size() - 1, pageIndex.get() + delta));
        if (next == pageIndex.get()) return false;
        pageIndex.set(next);
        retarget();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level.isClientSide) return true;
        if (player.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5) > 64) return false;
        if (!MultiCrafterStructures.get(level, origin).valid()) return false;
        int index = pageIndex.get();
        return index < storages.size() && level.getBlockEntity(storages.get(index)) instanceof MultiCrafterStorageBlockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < SLOTS
                ? moveItemStackTo(stack, SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private static final class PatternSlot extends Slot {
        PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    /** The storage on the current page; switching pages swaps the target and the menu resyncs the slots. */
    private static final class PageContainer implements Container {
        static final Container EMPTY = new SimpleContainer(SLOTS) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return false;
            }
        };
        Container target = EMPTY;

        @Override public int getContainerSize() { return SLOTS; }
        @Override public boolean isEmpty() { return target.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return slot < target.getContainerSize() ? target.getItem(slot) : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return target.removeItem(slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return target.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { target.setItem(slot, stack); }
        @Override public int getMaxStackSize() { return target.getMaxStackSize(); }
        @Override public int getMaxStackSize(ItemStack stack) { return target.getMaxStackSize(stack); }
        @Override public void setChanged() { target.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return target.canPlaceItem(slot, stack); }
        @Override public void clearContent() { target.clearContent(); }
    }
}
