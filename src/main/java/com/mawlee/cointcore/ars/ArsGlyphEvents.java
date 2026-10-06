package com.mawlee.cointcore.ars;

import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Invalidates crush recipe cache when datapacks / tags reload.
 */
public final class ArsGlyphEvents {
    private ArsGlyphEvents() {
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            CrushRecipeCache.invalidate();
            ArsGlyphThrottle.clear();
        }
    }
}
