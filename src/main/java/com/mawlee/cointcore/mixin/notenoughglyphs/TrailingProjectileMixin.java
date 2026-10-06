package com.mawlee.cointcore.mixin.notenoughglyphs;

import alexthw.not_enough_glyphs.common.spell.TrailingProjectile;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TrailingProjectile.class, remap = false)
public abstract class TrailingProjectileMixin {
    @Shadow
    public abstract float getAoe();

    @Redirect(
            method = "castSpells",
            at = @At(
                    value = "INVOKE",
                    target = "Lalexthw/not_enough_glyphs/common/spell/TrailingProjectile;getAoe()F"
            ),
            remap = false
    )
    private float cointcore$capTrailAoe(TrailingProjectile projectile) {
        float aoe = ((TrailingProjectileMixin) (Object) projectile).getAoe();
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return aoe;
        }
        return Math.min(aoe, ArsGlyphPerfConfig.getFieldMaxRadius());
    }

    @Redirect(
            method = "castSpells",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"
            ),
            remap = false
    )
    private void cointcore$budgetTrailResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }
}
