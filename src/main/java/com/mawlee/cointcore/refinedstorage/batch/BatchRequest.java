package com.mawlee.cointcore.refinedstorage.batch;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import java.util.List;

/** Client-side description; the server independently validates and recalculates it. */
public record BatchRequest(int menuId, List<ResourceAmount> outputs, ResourceAmount recipeOutput) {
    public BatchRequest { outputs = List.copyOf(outputs); }
}
