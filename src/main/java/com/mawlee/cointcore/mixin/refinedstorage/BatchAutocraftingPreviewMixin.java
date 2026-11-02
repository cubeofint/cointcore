package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.batch.*;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.preview.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = AutocraftingPreviewContainerMenu.class, remap = false)
public abstract class BatchAutocraftingPreviewMixin {
    @Shadow @Final private List<AutocraftingRequest> requests;
    @Shadow private AutocraftingRequest currentRequest;
    @Shadow private AutocraftingPreviewStyle style;

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILjava/util/List;)V", at = @At("TAIL"), require = 1)
    private void cointcore$combine(MenuType<?> type, int syncId, List<AutocraftingRequest> original, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (cointcore$batch() != null) { style = AutocraftingPreviewStyle.LIST; return; }
        if (player == null || (BatchRecipeTarget.current() == null && !Screen.hasControlDown())) return;
        var combined = BatchRequestsClient.combine(requests, player.containerMenu.containerId);
        if (combined == null) return;
        requests.clear();
        requests.add(combined);
        currentRequest = combined;
        style = AutocraftingPreviewStyle.LIST;
        ((AutocraftingPreviewContainerMenu) (Object) this).getResourceSlots().getFirst().change(
                new com.refinedmods.refinedstorage.api.resource.ResourceAmount(
                        ((AutocraftingRequestAccessor) combined).cointcore$requestedResource(), 1));
    }

    @Unique private BatchRequest cointcore$batch() { return ((BatchRequestHolder) currentRequest).cointcore$batch(); }

    @Inject(method = "getMinAmount", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$minimum(CallbackInfoReturnable<Double> ci) {
        if (cointcore$batch() != null) ci.setReturnValue(1.0);
    }

    @Inject(method = "toggleStyle", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$list(double amount, CallbackInfoReturnable<AutocraftingPreviewStyle> ci) {
        if (cointcore$batch() != null) ci.setReturnValue(AutocraftingPreviewStyle.LIST);
    }

    @Inject(method = "requestMaxAmount", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$max(CallbackInfo ci) {
        if (cointcore$batch() != null) { cointcore$send(BatchCraftingPackets.MAX); ci.cancel(); }
    }

    @Inject(method = "sendCancelRequest", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$cancel(CallbackInfo ci) {
        if (cointcore$batch() != null) { cointcore$send(BatchCraftingPackets.CANCEL); ci.cancel(); }
    }

    @ModifyVariable(method = "maxAmountResponseReceived", at = @At("HEAD"), argsOnly = true, require = 1)
    private long cointcore$maxResponse(long amount) {
        var resource = ((AutocraftingRequestAccessor) currentRequest).cointcore$requestedResource();
        return cointcore$batch() != null && resource instanceof PlatformResourceKey key
                ? key.getResourceType().normalizeAmount((double) amount) : amount;
    }

    @Unique private void cointcore$send(int action) {
        var batch = cointcore$batch();
        PacketDistributor.sendToServer(new BatchCraftingPackets.Request(batch.menuId(),
                ((AutocraftingRequestAccessor) currentRequest).cointcore$requestId(), batch.outputs(), batch.recipeOutput(), 1, action, false));
    }
}
