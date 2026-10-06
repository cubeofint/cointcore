package com.mawlee.cointcore.adastra;

import com.mawlee.cointcore.mixin.adastra.AdAstraGravityListenersAccessor;
import earth.terrarium.adastra.api.events.AdAstraEvents;
import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Same decision as the old inject into Ad Astra's gravity tick: leave planet
 * gravity and normalizer volumes on the original handler, and skip the
 * velocity write when the result is still 1.
 */
public final class AdAstraGravityFastPath {
    private AdAstraGravityFastPath() {
    }

    public static boolean shouldSkipNeutralGravity(Entity self) {
        if (AdAstraConfig.disableGravity) {
            return true;
        }
        if (!(self.level() instanceof ServerLevel level)) {
            return false;
        }
        float dimensionGravity = AdAstraLocalOverrides.dimensionGravity(level);
        if (dimensionGravity != 1.0F) {
            return false;
        }
        int blockX = Mth.floor(self.getX());
        int blockZ = Mth.floor(self.getZ());
        long chunk = AdAstraLocalOverrides.chunkKey(blockX, blockZ);
        if (AdAstraLocalOverrides.hasCustomGravity(level, chunk)) {
            return false;
        }
        if (!AdAstraLocalOverrides.isGravityProbed(level, chunk)) {
            BlockPos eye = BlockPos.containing(self.getX(), self.getEyeY(), self.getZ());
            float stored = GravityApi.API.getGravity(level, eye);
            AdAstraLocalOverrides.markGravityProbed(level, chunk);
            if (stored != dimensionGravity) {
                AdAstraLocalOverrides.markCustomGravity(level, eye);
                return false;
            }
        }
        float gravity = dimensionGravity;
        List<AdAstraEvents.EntityGravityEvent> listeners = AdAstraGravityListenersAccessor.cointcore$gravityListeners();
        if (!listeners.isEmpty()) {
            gravity = AdAstraEvents.EntityGravityEvent.fire(self, gravity);
        }
        if (gravity == 1.0F) {
            return true;
        }
        Vec3 movement = self.getDeltaMovement();
        self.setDeltaMovement(
                movement.x,
                movement.y + 0.04D - 0.04D * (double) gravity,
                movement.z
        );
        return true;
    }
}
