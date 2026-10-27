package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.RecentOrdersClient;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewContainerMenu;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AutocraftingPreviewContainerMenu.class, remap = false)
public abstract class RecentOrdersRequestMixin {
    @Shadow private AutocraftingRequest currentRequest;

    @Inject(method = "responseReceived(Ljava/util/UUID;Z)V", at = @At("HEAD"), require = 1)
    private void cointcore$rememberAcceptedOrder(UUID id, boolean accepted, CallbackInfo callback) {
        var request = (AutocraftingRequestAccessor) currentRequest;
        if (accepted && request.cointcore$requestId().equals(id)) {
            RecentOrdersClient.HISTORY.remember(request.cointcore$requestedResource());
        }
    }
}
