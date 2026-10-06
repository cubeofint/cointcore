package com.mawlee.cointcore.mixin.mobgrindingutils;

import com.mawlee.cointcore.config.MachinePerfConfig;
import mob_grinding_utils.tile.TileEntitySaw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * MGU Saw rebuilds an enchanted FakePlayer weapon and runs full {@code attack()} on every living
 * entity in its AABB every 10 ticks. Cap interval and kills/pulse to cut server-thread cost.
 */
@Mixin(value = TileEntitySaw.class, remap = false)
public abstract class TileEntitySawMixin {
    @ModifyConstant(method = "serverTick", constant = @Constant(longValue = 10L), remap = false)
    private static long cointcore$sawInterval(long original) {
        if (!MachinePerfConfig.isMguSawEnabled()) {
            return original;
        }
        return MachinePerfConfig.getMguSawIntervalTicks();
    }

    @Redirect(
            method = "activateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = true
            ),
            remap = false
    )
    private <T extends Entity> List<T> cointcore$capSawTargets(Level level, Class<T> entityClass, AABB aabb) {
        List<T> entities = level.getEntitiesOfClass(entityClass, aabb);
        if (!MachinePerfConfig.isMguSawEnabled() || !LivingEntity.class.isAssignableFrom(entityClass)) {
            return entities;
        }

        int max = MachinePerfConfig.getMguSawMaxKillsPerPulse();
        if (entities.size() <= max) {
            return entities;
        }

        return new ArrayList<>(entities.subList(0, max));
    }
}
