package com.mawlee.cointcore.mixin.powah;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.PowahPerfConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owmii.powah.block.transmitter.PlayerTransmitterTile;
import owmii.powah.item.BindingCardItem;
import owmii.powah.util.ChargeUtil;

/**
 * Player Transmitter scans the full player inventory (+ Curios) every tick via
 * {@link ChargeUtil#chargeItemsInPlayerInv}. Throttle attempts and idle-skip when nothing charged.
 */
@Mixin(value = PlayerTransmitterTile.class, remap = false)
public abstract class PlayerTransmitterTileMixin {

    @Unique
    private int cointcore$intervalRemaining;

    @Unique
    private int cointcore$idleSkipRemaining;

    @Inject(method = "postTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttlePostTick(Level level, CallbackInfoReturnable<Integer> cir) {
        if (!PowahPerfConfig.isEnabled()) {
            return;
        }
        if (cointcore$idleSkipRemaining > 0) {
            cointcore$idleSkipRemaining--;
            cir.setReturnValue(-1);
            return;
        }
        if (cointcore$intervalRemaining > 0) {
            cointcore$intervalRemaining--;
            cir.setReturnValue(-1);
        }
    }

    @WrapOperation(
            method = "postTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lowmii/powah/util/ChargeUtil;chargeItemsInPlayerInv(Lnet/minecraft/world/entity/player/Player;JJ)J"
            ),
            remap = false
    )
    private long cointcore$afterChargeAttempt(
            Player player,
            long chargingSpeed,
            long energyStored,
            Operation<Long> original
    ) {
        long charged = original.call(player, chargingSpeed, energyStored);
        if (!PowahPerfConfig.isEnabled()) {
            return charged;
        }

        PlayerTransmitterTile self = (PlayerTransmitterTile) (Object) this;
        boolean multiDim = false;
        ItemStack cardStack = self.getInventory().getFirst();
        if (cardStack.getItem() instanceof BindingCardItem card) {
            multiDim = card.isMultiDim(cardStack);
        }

        int interval = PowahPerfConfig.resolveIntervalTicks(multiDim);
        cointcore$intervalRemaining = Math.max(0, interval - 1);

        if (charged <= 0L) {
            cointcore$idleSkipRemaining = PowahPerfConfig.resolveIdleSkipTicks();
        } else {
            cointcore$idleSkipRemaining = 0;
        }
        return charged;
    }
}
