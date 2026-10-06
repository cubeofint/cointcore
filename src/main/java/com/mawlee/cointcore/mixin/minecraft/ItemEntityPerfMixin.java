package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.config.ItemPerfConfig;
import com.mawlee.cointcore.item.ItemEntitySleep;
import com.mawlee.cointcore.item.ItemPiles;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sleep settled / still-water item entities far from players (see {@code item-perf.json}).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPerfMixin {
    @Unique
    private int cointcore$waterStillTicks;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cointcore$sleepFarItems(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemPiles.tick(self);
        cointcore$updateWaterStillStreak(self);
        if (ItemEntitySleep.trySleepTick(self, cointcore$waterStillTicks)) {
            ci.cancel();
        }
    }

    @Unique
    private void cointcore$updateWaterStillStreak(ItemEntity item) {
        if (item.level().isClientSide || !item.isInWaterOrBubble() || item.isInLava()) {
            cointcore$waterStillTicks = 0;
            return;
        }
        Vec3 motion = item.getDeltaMovement();
        double motionSqr = motion.x * motion.x + motion.y * motion.y + motion.z * motion.z;
        if (motionSqr <= ItemPerfConfig.stillMotionThresholdSqr()) {
            if (cointcore$waterStillTicks < Integer.MAX_VALUE) {
                cointcore$waterStillTicks++;
            }
        } else {
            cointcore$waterStillTicks = 0;
        }
    }
}
