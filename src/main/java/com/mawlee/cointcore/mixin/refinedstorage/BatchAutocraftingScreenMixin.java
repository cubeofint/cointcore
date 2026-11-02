package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mawlee.cointcore.refinedstorage.batch.BatchRequestHolder;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewContainerMenu;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewScreen;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AutocraftingPreviewScreen.class, remap = false)
public abstract class BatchAutocraftingScreenMixin {
    @ModifyExpressionValue(method = "<init>(Lcom/refinedmods/refinedstorage/common/autocrafting/preview/AutocraftingPreviewContainerMenu;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/world/entity/player/Inventory;)V",
            at = @At(value = "FIELD", target = "Lcom/refinedmods/refinedstorage/common/autocrafting/preview/AutocraftingPreviewScreen;TITLE:Lnet/minecraft/network/chat/MutableComponent;"), require = 1)
    private static MutableComponent cointcore$batchTitle(MutableComponent original,
            @Local(argsOnly = true) AutocraftingPreviewContainerMenu menu) {
        var request = ((BatchPreviewMenuAccessor) menu).cointcore$currentRequest();
        var batch = ((BatchRequestHolder) request).cointcore$batch();
        return batch == null ? original : Component.translatable("gui.cointcore.batch.title", batch.outputs().size());
    }
}
