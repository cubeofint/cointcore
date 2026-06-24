package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionPacket", remap = false)
public abstract class SuperGlueSelectionPacketMixin {
    @Shadow(remap = false)
    @Final
    private BlockPos from;

    @Shadow(remap = false)
    @Final
    private BlockPos to;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$guardSuperGlue(ServerPlayer player, CallbackInfo ci) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        Level level = player.level();

        if (!ClaimGuard.canInteract(player, level, to) || !ClaimGuard.canInteract(player, level, from)) {
            ci.cancel();
            return;
        }

        AABB span = AABB.encapsulatingFullBlocks(from, to);
        if (!ClaimGuard.canEditBox(player, level, span)) {
            ci.cancel();
        }
    }
}
