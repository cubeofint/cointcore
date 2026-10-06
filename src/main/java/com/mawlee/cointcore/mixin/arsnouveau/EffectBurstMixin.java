package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBurst;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EffectBurst.class, remap = false)
public abstract class EffectBurstMixin {
    @Redirect(
            method = "makeSphere",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;getAoeMultiplier()D"
            ),
            remap = false
    )
    private double cointcore$capBurstRadius(SpellStats stats) {
        double aoe = stats.getAoeMultiplier();
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return aoe;
        }
        int bonus = stats.isSensitive() ? 1 : 3;
        int maxAoe = Math.max(0, ArsGlyphPerfConfig.getBurstMaxRadius() - bonus);
        return Math.min(aoe, maxAoe);
    }

    @Redirect(
            method = "makeSphere",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"
            ),
            remap = false
    )
    private void cointcore$budgetBurstResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }
}
