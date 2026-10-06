package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityOrbitProjectile.class, remap = false)
public abstract class EntityOrbitProjectileMixin {
    @Redirect(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V",
                    remap = false
            ),
            remap = false
    )
    private void cointcore$throttleOrbitResolve(SpellResolver resolver, Level level, HitResult hit) {
        // Cast to Entity to avoid pulling GeckoLib GeoEntity through Ars entity hierarchy at compile time.
        int tickCount = ((Entity) (Object) this).tickCount;
        boolean blockHit = hit instanceof BlockHitResult;
        if (ArsGlyphPerfConfig.shouldSkipOrbitResolve(tickCount, blockHit)) {
            return;
        }
        resolver.onResolveEffect(level, hit);
    }

    @Inject(method = "getExpirationTime", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$capOrbitLifetime(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ArsGlyphPerfConfig.capOrbitLifetime(cir.getReturnValueI()));
    }
}
