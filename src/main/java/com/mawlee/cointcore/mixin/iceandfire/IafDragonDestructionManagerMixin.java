package com.mawlee.cointcore.mixin.iceandfire;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IafDragonDestructionManager.class, remap = false)
public abstract class IafDragonDestructionManagerMixin {
    @Inject(
            method = "attackBlock(Lnet/minecraft/world/level/Level;Lcom/iafenvoy/iceandfire/entity/DragonBaseEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void cointcore$guardAttackBlock(
            Level level,
            DragonBaseEntity dragon,
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (!ClaimGuard.canMobGriefAt(dragon, level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "lambda$destroyBlocks$6", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$guardDestroyBlocks(
            BlockPos center,
            double range,
            Level level,
            Entity entity,
            BlockPos target,
            CallbackInfo ci
    ) {
        if (!ClaimGuard.canMobGriefAt(entity, level, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "lambda$destroyAreaCharge$3", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$guardChargeDestroy(
            Level level,
            DragonBaseEntity dragon,
            BlockPos center,
            BlockPos target,
            CallbackInfo ci
    ) {
        if (!ClaimGuard.canMobGriefAt(dragon, level, target)) {
            ci.cancel();
        }
    }
}
