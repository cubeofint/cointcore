package com.mawlee.cointcore.mixin.ae2;

import appeng.spatial.SpatialStorageHelper;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = SpatialStorageHelper.class, remap = false)
public abstract class SpatialStorageHelperMixin {
    @Inject(method = "swapRegions", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$guardSwapRegions(
            ServerLevel levelA,
            int xA,
            int yA,
            int zA,
            ServerLevel levelB,
            int xB,
            int yB,
            int zB,
            int sizeX,
            int sizeY,
            int sizeZ,
            CallbackInfo ci
    ) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        BlockPos anchor = new BlockPos(xA, yA, zA);
        Entity actor = ClaimGuard.resolveActor(levelA, anchor, Optional.empty());
        if (actor == null) {
            return;
        }

        AABB boxA = new AABB(xA, yA, zA, xA + sizeX + 1, yA + sizeY + 1, zA + sizeZ + 1);
        AABB boxB = new AABB(xB, yB, zB, xB + sizeX + 1, yB + sizeY + 1, zB + sizeZ + 1);

        if (!ClaimGuard.canEditBox(actor, levelA, boxA) || !ClaimGuard.canEditBox(actor, levelB, boxB)) {
            ci.cancel();
        }
    }
}
