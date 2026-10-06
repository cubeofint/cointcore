package com.mawlee.cointcore.mixin.iceandfire;

import com.iafenvoy.iceandfire.entity.CyclopsEntity;
import com.iafenvoy.iceandfire.entity.SeaSerpentEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {CyclopsEntity.class, SeaSerpentEntity.class}, remap = false)
public abstract class IafBoundingBoxBreakMixin {
    @WrapOperation(
            method = "breakBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
            ),
            remap = false
    )
    private boolean cointcore$guardHitboxDestroy(
            Level level,
            BlockPos pos,
            boolean dropItems,
            Operation<Boolean> original
    ) {
        if (!ClaimGuard.canMobGriefAt((Entity) (Object) this, level, pos)) {
            return false;
        }
        return original.call(level, pos, dropItems);
    }
}
