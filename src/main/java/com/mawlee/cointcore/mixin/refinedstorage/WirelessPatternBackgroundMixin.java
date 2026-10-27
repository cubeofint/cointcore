package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternClient;
import com.refinedmods.refinedstorage.common.support.stretching.AbstractStretchingScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractStretchingScreen.class, remap = false)
public abstract class WirelessPatternBackgroundMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), require = 1)
    private void cointcore$drawWirelessTabs(GuiGraphics graphics, int x, int y, CallbackInfo callback) {
        WirelessPatternClient.renderTabBackgrounds((AbstractContainerScreen<?>) (Object) this, graphics);
    }
}
