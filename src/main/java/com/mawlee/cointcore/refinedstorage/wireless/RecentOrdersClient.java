package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

public final class RecentOrdersClient {
    private RecentOrdersClient() { }
    public static void remember(ResourceKey resource) {
        if (resource instanceof PlatformResourceKey key) {
            var board = PinStorage.board();
            long before = board.version();
            board.rememberCraft(key);
            if (before != board.version()) PinStorage.save();
        }
    }
}
