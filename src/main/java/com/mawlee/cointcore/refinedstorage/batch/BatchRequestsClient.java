package com.mawlee.cointcore.refinedstorage.batch;

import com.mawlee.cointcore.mixin.refinedstorage.AutocraftingRequestAccessor;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import java.util.ArrayList;
import java.util.List;

public final class BatchRequestsClient {
    private BatchRequestsClient() { }

    public static AutocraftingRequest combine(List<AutocraftingRequest> requests, int menuId) {
        return combine(requests, menuId, BatchRecipeTarget.current());
    }

    public static AutocraftingRequest combine(List<AutocraftingRequest> requests, int menuId, ResourceAmount recipeOutput) {
        if (requests.isEmpty() || (requests.size() == 1 && recipeOutput == null)
                || requests.size() > BatchCraftingPlanner.MAX_REQUESTS) return null;
        var outputs = new ArrayList<ResourceAmount>();
        for (var request : requests) {
            var accessor = (AutocraftingRequestAccessor) request;
            if (!(accessor.cointcore$requestedResource() instanceof PlatformResourceKey resource)) return null;
            long amount = resource.getResourceType().normalizeAmount(accessor.cointcore$requestedAmount());
            if (amount <= 0) return null;
            outputs.add(new ResourceAmount(resource, amount));
        }
        var first = recipeOutput != null && recipeOutput.resource() instanceof PlatformResourceKey key
                ? key : (PlatformResourceKey) outputs.getFirst().resource();
        var combined = AutocraftingRequest.of(new ResourceAmount(first, first.getResourceType().normalizeAmount(1)));
        ((BatchRequestHolder) combined).cointcore$batch(new BatchRequest(menuId, outputs, recipeOutput));
        return combined;
    }
}
