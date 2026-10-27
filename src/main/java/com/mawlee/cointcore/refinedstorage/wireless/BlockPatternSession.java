package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Per-viewer preview. Virtual blanks never enter the block's inventory, NBT or drops. */
public final class BlockPatternSession {
    private static final ThreadLocal<BlockPatternSession> ENCODING = new ThreadLocal<>();
    private final PatternGridBlockEntity grid;
    private final ServerPlayer player;
    private final SimpleContainer preview = new SimpleContainer(1);
    private long available;

    public BlockPatternSession(PatternGridBlockEntity grid, ServerPlayer player) {
        this.grid = grid;
        this.player = player;
        // Preserve blanks left in the physical slot by an earlier version.
        ItemStack previous = grid.getPatternInput().removeItemNoUpdate(0);
        if (!previous.isEmpty()) {
            grid.setChanged();
            player.getInventory().placeItemBackInInventory(previous);
        }
        refresh();
    }

    private ItemResource blank() {
        return new ItemResource(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("refinedstorage", "pattern")));
    }

    public SimpleContainer preview() { return preview; }
    public long available() { return available; }

    private boolean canUse() {
        return player.isAlive() && grid.canMenuStayOpen(player) && grid.isGridActive();
    }

    public void refresh() {
        var blank = blank();
        available = canUse() ? grid.getItemStorage().extract(blank, Long.MAX_VALUE, Action.SIMULATE,
                new PlayerActor(player)) : 0;
        ItemStack stack = available > 0 ? blank.toItemStack(1) : ItemStack.EMPTY;
        if (!ItemStack.matches(preview.getItem(0), stack)) preview.setItem(0, stack);
    }

    public boolean consume() {
        if (!canUse()) return false;
        var blank = blank();
        var destination = new LimitedStorageImpl(1);
        // Native wired operations enforce RS security using the requesting player.
        grid.createOperations(blank.getResourceType(), player)
                .extract(blank, GridExtractMode.SINGLE_RESOURCE, destination);
        return destination.getStored() == 1;
    }

    public void encode(Runnable nativeEncoding) {
        if (!canUse()) return;
        refresh();
        var previous = ENCODING.get();
        ENCODING.set(this);
        try {
            nativeEncoding.run();
        } finally {
            if (previous == null) ENCODING.remove();
            else ENCODING.set(previous);
            refresh();
        }
    }

    public static BlockPatternSession encoding(Object grid) {
        var session = ENCODING.get();
        return session != null && session.grid == grid ? session : null;
    }
}
