package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityLingeringSpell;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityLingeringSpell.class, remap = false)
public abstract class EntityLingeringSpellMixin {
    @Shadow
    public abstract float getAoe();

    @Redirect(
            method = "castSpells",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityLingeringSpell;getAoe()F"
            ),
            remap = false
    )
    private float cointcore$capLingerAoe(EntityLingeringSpell spell) {
        float aoe = ((EntityLingeringSpellMixin) (Object) spell).getAoe();
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
    private void cointcore$budgetLingerResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }
}
