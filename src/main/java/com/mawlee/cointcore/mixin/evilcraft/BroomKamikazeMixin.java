package com.mawlee.cointcore.mixin.evilcraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Kamikaze detonates with {@link Level.ExplosionInteraction#TNT}, which destroys terrain.
 */
@Mixin(targets = "org.cyclops.evilcraft.api.broom.BroomModifiers$5", remap = false)
public abstract class BroomKamikazeMixin {

    @Redirect(
            method = "onCollide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
            ),
            remap = true
    )
    private Explosion cointcore$kamikazeNoTerrain(
            Level level,
            Entity source,
            double x,
            double y,
            double z,
            float radius,
            Level.ExplosionInteraction interaction
    ) {
        return level.explode(source, x, y, z, radius, Level.ExplosionInteraction.NONE);
    }
}
