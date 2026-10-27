package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.storage.AbstractImmutableConfiguredProxyStorage;
import com.refinedmods.refinedstorage.api.network.impl.storage.NetworkNodeStorageConfiguration;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.StorageState;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InfinityResourceStorageTest {
    private final ResourceKey resource = new ResourceKey() { };
    private final ResourceKey other = new ResourceKey() { };
    private final InfinityResourceStorage source = new InfinityResourceStorage(resource, null);

    @Test
    void missingOptionalResourceCannotBecomeAnAirSource() {
        var resourceRef = new java.util.concurrent.atomic.AtomicReference<ResourceKey>();
        var type = new InfinityDiskStorageType(resourceRef::get, () -> null);
        assertFalse(type.isAvailable());
        assertFalse(type.isAllowed(resource));
        assertFalse(type.isAllowed(null));
        assertThrows(NullPointerException.class, type::createSource);
        resourceRef.set(resource);
        assertTrue(type.isAvailable());
        assertTrue(type.isAllowed(resource));
        assertEquals(64, type.createSource().extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
    }

    @Test
    void extractionNeverConsumesAndSimulationDoesNotMutate() {
        for (Action action : Action.values()) {
            assertEquals(64, source.extract(resource, 64, action, Actor.EMPTY));
            assertEquals(0, source.extract(other, 64, action, Actor.EMPTY));
            assertEquals(0, source.extract(resource, -1, action, Actor.EMPTY));
            assertEquals(0, source.insert(resource, 64, action, Actor.EMPTY));
        }
        assertEquals(0, source.getStored());
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT, source.getAll().iterator().next().amount());
    }

    @Test
    void virtualResourcesDoNotFillTheDriveOrReportFullDiskState() {
        var disk = new InfinityStateTrackedStorage(source, () -> fail("Source state must remain constant"));
        assertEquals(0, disk.getStored());
        assertEquals(0, disk.getCapacity());
        assertEquals(StorageState.NORMAL, disk.getState());
        assertEquals(new StorageInfo(0, 0), StorageInfo.of(source));
        assertEquals(new StorageInfo(0, 0), StorageInfo.of(disk));
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT, disk.getAll().iterator().next().amount());
        disk.extract(resource, 64, Action.EXECUTE, Actor.EMPTY);
        assertEquals(StorageState.NORMAL, disk.getState());
        assertEquals(0, disk.getStored());

        var ordinary = new com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl(100);
        ordinary.insert(other, 25, Action.EXECUTE, Actor.EMPTY);
        var ordinaryDisk = new StateTrackedStorage(ordinary, () -> { });
        assertEquals(25, disk.getStored() + ordinaryDisk.getStored());
        assertEquals(100, disk.getCapacity() + ordinaryDisk.getCapacity());
    }

    @Test
    void nestedNetworkCachesStayConstantAndDiskRemovalClearsTheSource() {
        var driveList = MutableResourceListImpl.create();
        var drive = new CompositeStorageImpl(driveList);
        var networkList = MutableResourceListImpl.create();
        var network = new CompositeStorageImpl(networkList);
        var disk = new InfinityStateTrackedStorage(source, () -> fail("Source state must remain constant"));
        drive.addSource(disk);
        network.addSource(new ConfiguredDrive(drive));
        for (int i = 0; i < 100; i++) {
            assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        }
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT, advertisedAmount(drive));
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT, advertisedAmount(network));
        assertEquals(0, network.extract(other, 64, Action.EXECUTE, Actor.EMPTY));
        drive.removeSource(disk);
        assertEquals(0, advertisedAmount(network));
        assertEquals(0, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
    }

    @Test
    void duplicateDisksAndOrdinaryStorageDoNotLeavePhantomResources() {
        var network = new CompositeStorageImpl(MutableResourceListImpl.create());
        var first = new InfinityStateTrackedStorage(source, () -> { });
        var second = new InfinityStateTrackedStorage(source, () -> { });
        var ordinary = new StorageImpl();
        ordinary.insert(other, 10, Action.EXECUTE, Actor.EMPTY);
        network.addSource(first);
        network.addSource(second);
        network.addSource(new StateTrackedStorage(ordinary, () -> { }));
        assertEquals(2 * InfinityResourceStorage.DISPLAY_AMOUNT + 10, advertisedAmount(network));
        assertEquals(5, network.extract(other, 5, Action.EXECUTE, Actor.EMPTY));
        assertEquals(64, network.extract(resource, 64, Action.SIMULATE, Actor.EMPTY));
        network.removeSource(first);
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT + 5, advertisedAmount(network));
        network.removeSource(second);
        assertEquals(5, advertisedAmount(network));
    }

    @Test
    void emptyFiniteDiskAcceptsDepositsWithAndWithoutInfinitySources() {
        for (boolean withInfinity : new boolean[]{false, true}) {
            var ordinary = new com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl(65_536);
            var drive = new CompositeStorageImpl(MutableResourceListImpl.create());
            drive.addSource(new StateTrackedStorage(ordinary, () -> { }));
            if (withInfinity) drive.addSource(new InfinityStateTrackedStorage(source, () -> { }));
            var network = new CompositeStorageImpl(MutableResourceListImpl.create());
            network.addSource(new ConfiguredDrive(drive));

            assertEquals(64, network.insert(other, 64, Action.SIMULATE, Actor.EMPTY));
            assertEquals(0, ordinary.getStored());
            assertEquals(64, network.insert(other, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, ordinary.getStored());
            assertEquals(64, network.extract(other, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, ordinary.getStored());
            // Returning the infinite resource must also go into the finite disk, without voiding it.
            assertEquals(32, network.insert(resource, 32, Action.EXECUTE, Actor.EMPTY));
            assertEquals(32, ordinary.getStored());
            assertEquals(32 + (withInfinity ? InfinityResourceStorage.DISPLAY_AMOUNT : 0), advertisedAmount(network));
        }
    }

    @Test
    void finiteAndInfiniteCopiesOfTheSameResourceKeepCorrectRootAccounting() {
        var drive = new CompositeStorageImpl(MutableResourceListImpl.create());
        var ordinary = new StorageImpl();
        ordinary.insert(resource, 10, Action.EXECUTE, Actor.EMPTY);
        drive.addSource(new StateTrackedStorage(ordinary, () -> { }));
        var infinite = new InfinityStateTrackedStorage(source, () -> { });
        drive.addSource(infinite);
        var network = new CompositeStorageImpl(MutableResourceListImpl.create());
        var configured = new ConfiguredDrive(drive);
        network.addSource(configured);
        assertEquals(64, network.extract(resource, 64, Action.SIMULATE, Actor.EMPTY));
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT + 10, advertisedAmount(network));
        assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(InfinityResourceStorage.DISPLAY_AMOUNT, advertisedAmount(network));
        assertEquals(10, network.insert(resource, 10, Action.EXECUTE, Actor.EMPTY));
        assertEquals(10, ordinary.getStored());
        drive.removeSource(infinite);
        assertEquals(10, advertisedAmount(network));
        assertEquals(10, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
    }

    private static long advertisedAmount(com.refinedmods.refinedstorage.api.storage.Storage storage) {
        return storage.getAll().stream().mapToLong(entry -> entry.amount()).sum();
    }

    /** Mirrors RS ExposedStorage, including its configured proxy between the drive and root list. */
    private static final class ConfiguredDrive extends AbstractImmutableConfiguredProxyStorage<CompositeStorageImpl> {
        ConfiguredDrive(CompositeStorageImpl drive) {
            super(new NetworkNodeStorageConfiguration(new AbstractNetworkNode() {
                @Override public long getEnergyUsage() { return 0; }
                @Override public boolean isActive() { return true; }
            }), drive);
        }

        @Override
        public Amount compositeExtract(ResourceKey resource, long amount, Action action, Actor actor) {
            if (action != Action.EXECUTE) {
                return super.compositeExtract(resource, amount, action, actor);
            }
            return InfinityDiskTransfers.extract(getDelegate(), resource,
                    () -> super.compositeExtract(resource, amount, action, actor));
        }

        @Override public void onAddedIntoComposite(ParentComposite parent) {
            getDelegate().onAddedIntoComposite(parent);
        }

        @Override public void onRemovedFromComposite(ParentComposite parent) {
            getDelegate().onRemovedFromComposite(parent);
        }
    }
}
