package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/** Visible ingredient regions from the native preview's most recent render. */
public interface PreviewIngredients {
    List<Hit> cointcore$previewIngredients();

    record Hit(ResourceKey resource, Rect2i area) { }
}
