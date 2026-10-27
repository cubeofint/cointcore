package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.api.autocrafting.preview.TreePreview;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.network.node.grid.GridOperations;
import com.refinedmods.refinedstorage.api.network.node.grid.GridWatcher;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.TrackedResourceAmount;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** In-memory pattern editor. Never placed, ticked, registered in a chunk or added to the RS network. */
public final class WirelessPatternBackend extends PatternGridBlockEntity {
    private final WirelessPatternSession session;
    private final Grid wireless;
    private final NetworkItemContext context;

    WirelessPatternBackend(WirelessPatternSession session, Grid wireless, NetworkItemContext context) {
        super(BlockPos.ZERO, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("refinedstorage", "pattern_grid")).defaultBlockState());
        this.session = session;
        this.wireless = wireless;
        this.context = context;
    }

    void refreshNetwork() {
        // Native pattern recipe transfer uses this reference only for choosing available ingredients.
        mainNetworkNode.setNetwork(context.resolveNetwork().orElse(null));
    }

    @Override public void setChanged() {
        // Constructor/load callbacks occur before the session is fully initialized.
        if (session != null) session.save();
    }

    @Override public boolean canMenuStayOpen(Player player) { return session.canUse(); }
    @Override public boolean isGridActive() { return wireless.isGridActive(); }
    @Override public Storage getItemStorage() { return wireless.getItemStorage(); }
    @Override public void addWatcher(GridWatcher watcher, Class<? extends Actor> type) { wireless.addWatcher(watcher, type); }
    @Override public void removeWatcher(GridWatcher watcher) { wireless.removeWatcher(watcher); }
    @Override public List<TrackedResourceAmount> getResources(Class<? extends Actor> type) { return wireless.getResources(type); }
    @Override public Set<PlatformResourceKey> getAutocraftableResources() { return wireless.getAutocraftableResources(); }
    @Override public GridOperations createOperations(ResourceType type, ServerPlayer player) { return wireless.createOperations(type, player); }
    @Override public CompletableFuture<Optional<Preview>> getPreview(ResourceKey resource, long amount, CancellationToken token) {
        return wireless.getPreview(resource, amount, token);
    }
    @Override public CompletableFuture<Optional<TreePreview>> getTreePreview(ResourceKey resource, long amount, CancellationToken token) {
        return wireless.getTreePreview(resource, amount, token);
    }
    @Override public CompletableFuture<Long> getMaxAmount(ResourceKey resource, CancellationToken token) {
        return wireless.getMaxAmount(resource, token);
    }
    @Override public Optional<TaskId> startTask(ResourceKey resource, long amount, Actor actor, boolean notify, CancellationToken token) {
        return wireless.startTask(resource, amount, actor, notify, token);
    }
}
