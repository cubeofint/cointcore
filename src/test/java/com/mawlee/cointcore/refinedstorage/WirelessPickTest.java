package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.PickBuffer;
import com.mawlee.cointcore.refinedstorage.wireless.PickSlotPlan;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WirelessPickTest {
    private final ResourceKey wanted = new ResourceKey() { };
    private final ResourceKey variant = new ResourceKey() { };

    @Test void extractionBufferAcceptsMultipleContributionsUpToOneStackWithoutMutationDuringSimulation() {
        var buffer = new PickBuffer(wanted, 64);
        assertEquals(64, buffer.insert(wanted, 200, Action.SIMULATE, Actor.EMPTY));
        assertEquals(0, buffer.getStored());
        assertEquals(40, buffer.insert(wanted, 40, Action.EXECUTE, Actor.EMPTY));
        assertEquals(24, buffer.insert(wanted, 40, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, buffer.insert(wanted, 40, Action.EXECUTE, Actor.EMPTY));
        assertEquals(64, buffer.getStored());
    }

    @Test void componentVariantsAndIncompatibleOrFullDestinationsCannotConsumeItems() {
        var buffer = new PickBuffer(wanted, 64);
        assertEquals(0, buffer.insert(variant, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, buffer.getStored());
        var full = new PickBuffer(wanted, 0);
        assertEquals(0, full.insert(wanted, 64, Action.EXECUTE, Actor.EMPTY));
    }

    @Test void fewerThanAStackInTheNetworkReturnsOnlyAvailableItems() {
        var first = new StorageImpl();
        var second = new StorageImpl();
        first.insert(wanted, 12, Action.EXECUTE, Actor.EMPTY);
        second.insert(wanted, 7, Action.EXECUTE, Actor.EMPTY);
        var network = new CompositeStorageImpl(MutableResourceListImpl.create());
        network.addSource(first);
        network.addSource(second);
        long available = network.extract(wanted, 64, Action.EXECUTE, Actor.EMPTY);
        var buffer = new PickBuffer(wanted, 64);
        assertEquals(19, buffer.insert(wanted, available, Action.EXECUTE, Actor.EMPTY));
        assertEquals(19, buffer.getStored());
        assertEquals(0, first.getStored() + second.getStored());
    }

    @Test void selectedCompatibleSlotIsPreferredAndOccupiedItemsArePreserved() {
        assertEquals(new PickSlotPlan(4, -1), PickSlotPlan.choose(4, slot -> false, slot -> slot == 4));
        assertEquals(new PickSlotPlan(6, -1), PickSlotPlan.choose(4, slot -> slot == 6, slot -> slot == 6));
        assertEquals(new PickSlotPlan(4, 15), PickSlotPlan.choose(4, slot -> slot == 15, slot -> false));
        assertEquals(new PickSlotPlan(-1, -1), PickSlotPlan.choose(4, slot -> false, slot -> false));
    }
}
