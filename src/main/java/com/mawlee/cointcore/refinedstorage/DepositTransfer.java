package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import java.util.function.Consumer;
import java.util.function.LongFunction;

/** Extract only planned capacity, then restore any part the destination could not actually accept. */
public final class DepositTransfer {
    private DepositTransfer() { }

    public static long transfer(Storage destination, ResourceAmount offered, Actor actor,
                                LongFunction<ResourceAmount> extract, Consumer<ResourceAmount> refund) {
        long planned = Math.min(offered.amount(), destination.insert(offered.resource(), offered.amount(), Action.SIMULATE, actor));
        if (planned <= 0) return 0;
        ResourceAmount removed = extract.apply(planned);
        if (removed == null || removed.amount() <= 0) return 0;
        long accepted = destination.insert(removed.resource(), removed.amount(), Action.EXECUTE, actor);
        if (accepted < removed.amount()) refund.accept(new ResourceAmount(removed.resource(), removed.amount() - accepted));
        return accepted;
    }
}
