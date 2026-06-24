package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.mawlee.cointcore.claim.ClaimGuardContext;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CartAssemblerBlockEntity.class, remap = false)
public abstract class CartAssemblerAssembleMixin {
    @Inject(
            method = "assemble",
            at = @At("HEAD"),
            remap = false
    )
    private void cointcore$pushActor(Level level, BlockPos pos, AbstractMinecart cart, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            ClaimGuard.resolveMinecartAssemblerActor(serverLevel, pos)
                    .ifPresent(ClaimGuardContext::pushActor);
        }
    }

    @Inject(
            method = "assemble",
            at = @At("RETURN"),
            remap = false
    )
    private void cointcore$popActor(Level level, BlockPos pos, AbstractMinecart cart, CallbackInfo ci) {
        ClaimGuardContext.popActor();
    }
}
