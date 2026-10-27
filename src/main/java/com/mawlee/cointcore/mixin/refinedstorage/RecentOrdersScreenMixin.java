package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.RecentOrdersClient;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractGridScreen.class, remap = false)
public abstract class RecentOrdersScreenMixin {
    @Unique private long cointcore$historyVersion = -1;
    @Shadow private void updateScrollbar() { throw new AssertionError(); }

    @Inject(method = "containerTick", at = @At("RETURN"), require = 1)
    private void cointcore$updateOrderRows(CallbackInfo callback) {
        long version = RecentOrdersClient.HISTORY.version();
        if (cointcore$historyVersion != version) {
            cointcore$historyVersion = version;
            updateScrollbar();
        }
    }
}
