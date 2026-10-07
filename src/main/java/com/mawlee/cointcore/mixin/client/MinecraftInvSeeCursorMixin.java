package com.mawlee.cointcore.mixin.client;

import com.mawlee.cointcore.invsee.client.InvSeeCursorPreserve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftInvSeeCursorMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void cointcore$saveInvSeeCursor(Screen screen, CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        InvSeeCursorPreserve.captureIfInvSee(self.screen, screen);
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void cointcore$restoreInvSeeCursor(Screen screen, CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        InvSeeCursorPreserve.restoreIfInvSee(self.screen);
    }
}
