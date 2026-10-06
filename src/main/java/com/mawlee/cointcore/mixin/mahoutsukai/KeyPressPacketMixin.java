package com.mawlee.cointcore.mixin.mahoutsukai;

import com.llamalad7.mixinextras.sugar.Local;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stepsword.mahoutsukai.networking.KeyPressPacket;

/**
 * Blood circles are placed by a keybind packet via {@code setBlockAndUpdate},
 * so FTB never sees a place event. Without this check they go down in foreign
 * claims and then cannot be broken.
 */
@Mixin(value = KeyPressPacket.class, remap = false)
public abstract class KeyPressPacketMixin {
    @Inject(
            method = "drawMahoujin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            cancellable = true,
            remap = false
    )
    private static void cointcore$guardDrawMahoujin(
            IPayloadContext context,
            CallbackInfo ci,
            @Local Player player,
            @Local(ordinal = 1) BlockPos placePos
    ) {
        if (!ClaimGuard.isAvailable() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ClaimGuard.canEdit(serverPlayer, serverPlayer.level(), placePos)) {
            ci.cancel();
        }
    }
}
