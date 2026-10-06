package com.mawlee.cointcore.mixin.notenoughglyphs;

import alexthw.not_enough_glyphs.common.glyphs.propagators.PropagatePlane;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PropagatePlane.class, remap = false)
public abstract class PropagatePlaneMixin {
    @Redirect(
            method = "propagate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;getAoeMultiplier()D"
            ),
            remap = false
    )
    private double cointcore$capPlaneAoe(SpellStats stats) {
        double aoe = stats.getAoeMultiplier();
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return aoe;
        }
        return Math.min(aoe, Math.max(0, ArsGlyphPerfConfig.getPlaneMaxRadius() - 1));
    }

    @Redirect(
            method = "propagate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;getBuffCount(Lcom/hollingsworth/arsnouveau/api/spell/AbstractAugment;)I"
            ),
            remap = false
    )
    private int cointcore$capPlanePierce(SpellStats stats, AbstractAugment augment) {
        int count = stats.getBuffCount(augment);
        if (!ArsGlyphPerfConfig.isEnabled() || augment != AugmentPierce.INSTANCE) {
            return count;
        }
        return Math.min(count, ArsGlyphPerfConfig.getPlaneMaxPierce());
    }

    @Redirect(
            method = {"circle", "cube"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"
            ),
            remap = false
    )
    private static void cointcore$budgetPlaneResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }
}
