package com.mawlee.cointcore.refinedstorage.batch;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;

/** Scoped to the synchronous recipe transfer on the client UI thread, never the last hovered recipe. */
public final class BatchRecipeTarget {
    private static ResourceAmount current;
    private BatchRecipeTarget() { }
    public static ResourceAmount current() { return current; }
    public static ResourceAmount replace(ResourceAmount target) {
        var previous = current;
        current = target;
        return previous;
    }
}
