package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class DepositTransferTest {
    private final ResourceKey item = new ResourceKey() { };

    @Test void fullNetworkDoesNotExtractAnythingFromTheBackpack() {
        long accepted = DepositTransfer.transfer(new LimitedStorageImpl(0), new ResourceAmount(item, 64), Actor.EMPTY,
                amount -> { fail("Full network must not touch backpack contents"); return null; },
                refund -> fail("Nothing should need a refund"));
        assertEquals(0, accepted);
    }

    @Test void partialCapacityKeepsUnacceptedItemsInTheSource() {
        var network = new LimitedStorageImpl(10);
        var source = new AtomicLong(64);
        long accepted = DepositTransfer.transfer(network, new ResourceAmount(item, 64), Actor.EMPTY,
                amount -> { source.addAndGet(-amount); return new ResourceAmount(item, amount); },
                refund -> source.addAndGet(refund.amount()));
        assertEquals(10, accepted);
        assertEquals(54, source.get());
        assertEquals(64, source.get() + network.getStored());
    }

    @Test void capacityChangesAfterSimulationRefundTheUnacceptedRemainder() {
        var network = new StorageImpl() {
            @Override public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
                return action == Action.SIMULATE ? amount : super.insert(resource, Math.min(3, amount), action, actor);
            }
        };
        var source = new AtomicLong(64);
        long accepted = DepositTransfer.transfer(network, new ResourceAmount(item, 64), Actor.EMPTY,
                amount -> { source.addAndGet(-amount); return new ResourceAmount(item, amount); },
                refund -> { assertEquals(item, refund.resource()); source.addAndGet(refund.amount()); });
        assertEquals(3, accepted);
        assertEquals(61, source.get());
        assertEquals(64, source.get() + network.getStored());
    }

    @Test void sourceMayYieldLessThanTheSimulatedAmountWithoutCreatingItems() {
        var network = new StorageImpl();
        assertEquals(2, DepositTransfer.transfer(network, new ResourceAmount(item, 64), Actor.EMPTY,
                amount -> new ResourceAmount(item, 2), refund -> fail("All extracted items fit")));
        assertEquals(2, network.getStored());
    }
}
