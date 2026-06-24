package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.simibubi.create.content.kinetics.deployer.DeployerHandler.class, remap = false)
public abstract class DeployerHandlerMixin {
    @Inject(method = "tryHarvestBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$guardHarvest(
            ServerPlayer player,
            ServerPlayerGameMode gameMode,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        if (!ClaimGuard.canEdit(player, player.level(), pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "safeOnUse", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$guardUse(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        if (!ClaimGuard.canInteract(player, level, pos) || !ClaimGuard.canEdit(player, level, pos)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
