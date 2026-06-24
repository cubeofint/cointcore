package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionAddBlocksMixin {
    @Shadow(remap = false)
    public AbstractContraptionEntity entity;

    @Shadow(remap = false)
    public abstract Map<BlockPos, ?> getBlocks();

    @Inject(method = "addBlocksToWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$guardAddBlocks(Level level, StructureTransform transform, CallbackInfo ci) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        BlockPos context = entity != null ? entity.blockPosition() : BlockPos.ZERO;
        Optional<UUID> controlling = entity != null ? entity.getControllingPlayer() : Optional.empty();
        Entity actor = ClaimGuard.resolveActor(level, context, controlling);
        if (actor == null) {
            return;
        }

        for (BlockPos localPos : getBlocks().keySet()) {
            BlockPos worldPos = transform.apply(localPos);
            if (!ClaimGuard.canEdit(actor, level, worldPos)) {
                ci.cancel();
                return;
            }
        }
    }
}
