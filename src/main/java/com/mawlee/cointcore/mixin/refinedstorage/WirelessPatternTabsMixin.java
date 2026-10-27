package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternMenu;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternIntegration;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.ultramega.universalgrid.common.ClientUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientUtils.class, remap = false)
public abstract class WirelessPatternTabsMixin {
    @Inject(method = "isUniversalGrid", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cointcore$customEditorTabs(AbstractBaseContainerMenu menu, Player player,
                                                 CallbackInfoReturnable<Boolean> callback) {
        // Replace the native tabs only for Universal Grid devices, including the pattern editor.
        if (menu instanceof WirelessPatternMenu wireless && wireless.cointcore$patternGridReference() != null) {
            callback.setReturnValue(false);
        }
        var reference = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
        if (reference != null && player != null
                && reference.resolve(player).filter(WirelessPatternIntegration::isUniversalGrid).isPresent()) {
            callback.setReturnValue(false);
        }
    }
}
