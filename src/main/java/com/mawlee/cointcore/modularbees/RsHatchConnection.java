package com.mawlee.cointcore.modularbees;

import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.mawlee.cointcore.config.ModularBeesRsConfig;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.NetworkNodeActor;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionSink;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionStrategy;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Supplier;

/** A normal wired RS node, exposed only on the same front face as the hatch's AE2 node. */
public final class RsHatchConnection {
    private final BlockEntity host;
    private final Supplier<Direction> facing;
    private final Supplier<IManagedGridNode> aeNode;
    private final AbstractNetworkNode node = new AbstractNetworkNode() {
        @Override public long getEnergyUsage() { return 1; }
    };
    private final NetworkNodeActor actor = new NetworkNodeActor(node);
    private final NetworkNodeContainerProvider provider = RefinedStorageApi.INSTANCE.createNetworkNodeContainerProvider();
    private boolean initialized;
    private final MEStorage storage = new StorageAdapter();

    public RsHatchConnection(BlockEntity host, Supplier<Direction> facing, Supplier<IManagedGridNode> aeNode) {
        this.host = host;
        this.facing = facing;
        this.aeNode = aeNode;
        node.setActive(true);
        var container = RefinedStorageApi.INSTANCE.createNetworkNodeContainer(host, node)
                .name(host.getBlockState().getBlock().getDescriptionId())
                .connectionStrategy(new ConnectionStrategy() {
                    @Override public void addOutgoingConnections(ConnectionSink sink) {
                        var side = facing.get();
                        sink.tryConnectInSameDimension(host.getBlockPos().relative(side), side.getOpposite());
                    }
                    @Override public boolean canAcceptIncomingConnection(Direction side, BlockState state) {
                        return side == facing.get();
                    }
                }).build();
        provider.addContainer(container);
    }

    public NetworkNodeContainerProvider provider(Direction side) {
        return initialized && (side == null || side == facing.get()) ? provider : null;
    }

    public void initialize() {
        if (initialized || host.getLevel() == null || host.getLevel().isClientSide || host.isRemoved()) return;
        initialized = true;
        provider.initialize(host.getLevel(), () -> { });
        host.getLevel().invalidateCapabilities(host.getBlockPos());
    }

    public void remove() {
        if (!initialized || host.getLevel() == null) return;
        initialized = false;
        // Hide the capability before rebuilding neighbours, otherwise RS may rediscover the same
        // still-present block during chunk unload / removal and re-add the container being removed.
        host.getLevel().invalidateCapabilities(host.getBlockPos());
        provider.remove(host.getLevel());
    }

    public void update() {
        if (initialized && host.getLevel() != null) {
            provider.update(host.getLevel());
            host.getLevel().invalidateCapabilities(host.getBlockPos());
        }
    }

    public boolean isPreferred() {
        return initialized && ModularBeesRsConfig.isEnabled() && !host.isRemoved() && !aeNode.get().isActive()
                && node.getNetwork() != null;
    }

    public boolean isDue() {
        return host.getLevel() != null && HatchTransferSchedule.isDue(host.getLevel().getGameTime(),
                host.getBlockPos().asLong(), ModularBeesRsConfig.getIntervalTicks());
    }

    public MEStorage storage() { return storage; }

    private StorageNetworkComponent root() {
        if (!isPreferred() || host.getLevel() == null || host.getLevel().isClientSide || !initialized) return null;
        var network = node.getNetwork();
        if (RefinedStorageApi.INSTANCE.isEnergyRequired()
                && network.getComponent(EnergyNetworkComponent.class).getStored() < node.getEnergyUsage()) return null;
        return network.getComponent(StorageNetworkComponent.class);
    }

    private static ResourceKey resource(AEKey key) {
        if (key instanceof AEItemKey item) return ItemResource.ofItemStack(item.getReadOnlyStack());
        if (key instanceof AEFluidKey fluid) {
            var stack = fluid.toStack(1);
            return new FluidResource(stack.getFluid(), stack.getComponentsPatch());
        }
        return null;
    }

    private final class StorageAdapter implements MEStorage {
        @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(key, amount, mode, source);
            var target = root();
            var resource = resource(key);
            return target == null || resource == null ? 0 : target.insert(resource, amount,
                    mode == Actionable.SIMULATE ? Action.SIMULATE : Action.EXECUTE, actor);
        }

        @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(key, amount, mode, source);
            var target = root();
            var resource = resource(key);
            return target == null || resource == null ? 0 : target.extract(resource, amount,
                    mode == Actionable.SIMULATE ? Action.SIMULATE : Action.EXECUTE, actor);
        }

        @Override public Component getDescription() { return Component.literal("Refined Storage"); }
    }
}
