package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectOrbit;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EffectOrbit.class, remap = false)
public abstract class EffectOrbitMixin {
    /**
     * Caps total Orbit projectiles ({@code 3 + Split}) by clamping the Split buff count.
     */
    @Redirect(
            method = {"onResolveBlock", "onResolveEntity"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;getBuffCount(Lcom/hollingsworth/arsnouveau/api/spell/AbstractAugment;)I",
                    remap = false
            ),
            remap = false
    )
    private int cointcore$capOrbitSplit(SpellStats stats, AbstractAugment augment) {
        int split = stats.getBuffCount(augment);
        int cappedTotal = ArsGlyphPerfConfig.capOrbitProjectiles(3 + split);
        return Math.max(0, cappedTotal - 3);
    }

    @Redirect(
            method = {"onResolveBlock", "onResolveEntity"},
            at = @At(
                    value = "FIELD",
                    target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityOrbitProjectile;extendTimes:I",
                    opcode = Opcodes.PUTFIELD,
                    remap = false
            ),
            remap = false
    )
    private void cointcore$capOrbitExtend(EntityOrbitProjectile projectile, int extendTimes) {
        projectile.extendTimes = ArsGlyphPerfConfig.capOrbitExtendTimes(extendTimes);
    }
}
