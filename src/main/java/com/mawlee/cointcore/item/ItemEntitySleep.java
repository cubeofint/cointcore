package com.mawlee.cointcore.item;

import com.mawlee.cointcore.config.ItemPerfConfig;
import com.mawlee.cointcore.mixin.accessor.ItemEntityAgeAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Puts settled floor / still-water items to sleep when no player is nearby,
 * skipping physics/merge/pickup scans. Age advances on a staggered maintenance tick.
 */
public final class ItemEntitySleep {
    /** Vanilla {@code ItemEntity} age marker for "never despawn". */
    private static final int UNLIMITED_LIFETIME_AGE = -32768;

    private ItemEntitySleep() {
    }

    /**
     * @param waterStillTicks consecutive ticks this item has been nearly still while in water
     * @return {@code true} if the normal {@link ItemEntity#tick()} body should be skipped
     */
    public static boolean trySleepTick(ItemEntity item, int waterStillTicks) {
        if (!ItemPerfConfig.isEnabled()) {
            return false;
        }
        Level level = item.level();
        if (level.isClientSide || item.isRemoved()) {
            return false;
        }
        if (item.getItem().isEmpty()) {
            return false;
        }
        if (item.getRemainingFireTicks() > 0 || item.isInLava()) {
            return false;
        }

        boolean inWater = item.isInWaterOrBubble();
        if (inWater && ItemPerfConfig.skipIfInFluid()) {
            return false;
        }

        Vec3 motion = item.getDeltaMovement();
        double motionSqr = motion.x * motion.x + motion.y * motion.y + motion.z * motion.z;
        boolean nearlyStill = motionSqr <= ItemPerfConfig.stillMotionThresholdSqr();

        ItemEntityAgeAccessor ageAccess = (ItemEntityAgeAccessor) item;
        int age = ageAccess.cointcore$getAge();

        if (inWater) {
            // Mob-farm puddles / source blocks: sleep after a short still-in-water streak.
            // Do not wait for minAgeBeforeSleep (that is for ground/air settle).
            if (!nearlyStill) {
                return false;
            }
            if (waterStillTicks < ItemPerfConfig.minWaterStillTicksBeforeSleep()) {
                return false;
            }
        } else {
            if (ItemPerfConfig.requireOnGround() && !item.onGround()) {
                return false;
            }
            if (age < ItemPerfConfig.minAgeBeforeSleep()) {
                return false;
            }
        }

        int interval = ItemPerfConfig.sleepTickInterval();
        int phase = Math.floorMod(item.getId() + item.tickCount, interval);
        if (phase != 0) {
            return true;
        }

        double radius = ItemPerfConfig.playerWakeRadius();
        if (radius > 0.0 && level.getNearestPlayer(item.getX(), item.getY(), item.getZ(), radius, false) != null) {
            return false;
        }

        if (age == UNLIMITED_LIFETIME_AGE) {
            return true;
        }
        int nextAge = age + interval;
        ageAccess.cointcore$setAge(nextAge);
        if (nextAge >= item.lifespan) {
            item.discard();
        }
        return true;
    }
}
