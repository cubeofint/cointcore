package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraVortexPass;
import earth.terrarium.adastra.common.entities.AirVortex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/**
 * Defers each vortex impulse to one shared entity walk at the end of the server tick.
 * The impulse itself is unchanged.
 */
@Mixin(value = AirVortex.class, remap = false)
public class AdAstraAirVortexMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private void cointcore$shareEntityPass(Iterable<Entity> entities, Consumer<Entity> consumer) {
        AdAstraVortexPass.join((ServerLevel) ((Entity) (Object) this).level(), consumer);
    }
}
