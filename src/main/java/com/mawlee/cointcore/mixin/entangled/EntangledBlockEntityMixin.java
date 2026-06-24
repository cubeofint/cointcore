package com.mawlee.cointcore.mixin.entangled;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.supermartijn642.entangled.EntangledBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntangledBlockEntity.class, remap = false)
public abstract class EntangledBlockEntityMixin {
    @Shadow(remap = false)
    public abstract boolean isBoundAndValid();

    @Shadow(remap = false)
    public abstract BlockPos getBoundBlockPos();

    @Shadow(remap = false)
    public abstract net.minecraft.resources.ResourceKey<Level> getBoundDimensionIdentifier();

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$guardBind(BlockPos boundPos, net.minecraft.resources.ResourceLocation dimension, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!ClaimGuard.isAvailable() || !(self.getLevel() instanceof ServerLevel sourceLevel)) {
            return;
        }

        ServerLevel boundLevel = sourceLevel.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension)
        );
        if (boundLevel == null) {
            return;
        }

        BlockPos sourcePos = self.getBlockPos();
        if (!ClaimGuard.canEntangledProxy(sourceLevel, sourcePos, boundLevel, boundPos)) {
            ci.cancel();
        }
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <A, C> void cointcore$guardCapability(
            BlockCapability<A, C> capability,
            C context,
            CallbackInfoReturnable<A> cir
    ) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!ClaimGuard.isAvailable() || !isBoundAndValid() || !(self.getLevel() instanceof ServerLevel sourceLevel)) {
            return;
        }

        ServerLevel boundLevel = sourceLevel.getServer().getLevel(getBoundDimensionIdentifier());
        if (boundLevel == null) {
            return;
        }

        BlockPos sourcePos = self.getBlockPos();
        BlockPos boundPos = getBoundBlockPos();
        if (!ClaimGuard.canAccessEntangledBound(sourceLevel, sourcePos, boundLevel, boundPos)) {
            cir.setReturnValue(null);
        }
    }
}
