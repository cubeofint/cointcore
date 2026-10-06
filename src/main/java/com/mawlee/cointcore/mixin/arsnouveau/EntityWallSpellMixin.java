package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityWallSpell;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityWallSpell.class, remap = false)
public abstract class EntityWallSpellMixin {
    @Shadow
    public abstract float getAoe();

    @Redirect(
            method = "castSpells",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityWallSpell;getAoe()F"
            ),
            remap = false
    )
    private float cointcore$capWallAoe(EntityWallSpell spell) {
        float aoe = ((EntityWallSpellMixin) (Object) spell).getAoe();
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
    private void cointcore$budgetWallResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }
}
