package com.mawlee.cointcore.mixin.irons;

import com.mawlee.cointcore.config.IronsSpectralHammerConfig;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft mixin: only applied when {@code irons_spellbooks} is loaded. Clamps the radius / depth the
 * Spectral Hammer spell hands to its entity, which flood-fills that whole box in one tick.
 *
 * <p>Method names verified against irons_spellbooks-1.21.1-3.16.3
 * ({@code SpectralHammerSpell#getRadius(int, LivingEntity)}, {@code #getDepth(int, LivingEntity)}).
 */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.spells.evocation.SpectralHammerSpell", remap = false)
public abstract class SpectralHammerSpellCapMixin {
    @Inject(method = "getRadius", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$capRadius(int spellLevel, LivingEntity caster, CallbackInfoReturnable<Integer> cir) {
        if (IronsSpectralHammerConfig.isEnabled() && cir.getReturnValueI() > IronsSpectralHammerConfig.getMaxRadius()) {
            cir.setReturnValue(IronsSpectralHammerConfig.getMaxRadius());
        }
    }

    @Inject(method = "getDepth", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$capDepth(int spellLevel, LivingEntity caster, CallbackInfoReturnable<Integer> cir) {
        if (IronsSpectralHammerConfig.isEnabled() && cir.getReturnValueI() > IronsSpectralHammerConfig.getMaxDepth()) {
            cir.setReturnValue(IronsSpectralHammerConfig.getMaxDepth());
        }
    }
}
