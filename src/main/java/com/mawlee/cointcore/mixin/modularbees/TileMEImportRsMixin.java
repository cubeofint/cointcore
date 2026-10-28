package com.mawlee.cointcore.mixin.modularbees;

import com.mawlee.cointcore.modularbees.RsHatchHost;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.glodblock.github.modularbees.xmod.ae.tileentities.TileMEImport", remap = false)
public abstract class TileMEImportRsMixin {
    @Shadow protected abstract void updateStorage();

    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$batchRsImport(Level level, BlockState state, CallbackInfo callback) {
        var connection = ((RsHatchHost) this).cointcore$rsHatch();
        if (!connection.isPreferred()) return;
        callback.cancel();
        if (((RsHatchHost) this).isActive() && connection.isDue()) updateStorage();
    }
}
