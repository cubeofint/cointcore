package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.batch.*;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewStyle;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value = AutocraftingRequest.class, remap = false)
public abstract class BatchAutocraftingRequestMixin implements BatchRequestHolder {
    @Shadow private UUID id;
    @Shadow private long pendingPreviewAmount;
    @Shadow private Preview preview;
    @Unique private BatchRequest cointcore$batch;

    @Override public BatchRequest cointcore$batch() { return cointcore$batch; }
    @Override public void cointcore$batch(BatchRequest request) { cointcore$batch = request; }

    @Inject(method = "sendPreviewRequest", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$preview(double amount, AutocraftingPreviewStyle style, CallbackInfoReturnable<Boolean> ci) {
        if (cointcore$batch == null) return;
        long multiplier = cointcore$multiplier(amount);
        if (pendingPreviewAmount == multiplier) { ci.setReturnValue(false); return; }
        pendingPreviewAmount = multiplier;
        preview = null;
        cointcore$send(multiplier, BatchCraftingPackets.PREVIEW, false);
        ci.setReturnValue(true);
    }

    @Inject(method = "sendRequest", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$start(double amount, boolean notify, CallbackInfo ci) {
        if (cointcore$batch == null) return;
        cointcore$send(cointcore$multiplier(amount), BatchCraftingPackets.START, notify);
        ci.cancel();
    }

    @Unique private void cointcore$send(long multiplier, int action, boolean notify) {
        PacketDistributor.sendToServer(new BatchCraftingPackets.Request(cointcore$batch.menuId(), id,
                cointcore$batch.outputs(), cointcore$batch.recipeOutput(), multiplier, action, notify));
    }

    @Unique private static long cointcore$multiplier(double amount) {
        return Double.isFinite(amount) ? Math.max(1, Math.min(BatchCraftingPlanner.MAX_MULTIPLIER, (long) amount)) : 1;
    }
}
