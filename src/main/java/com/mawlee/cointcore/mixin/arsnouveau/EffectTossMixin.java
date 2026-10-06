package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectToss;
import com.mawlee.cointcore.ars.ArsGlyphThrottle;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EffectToss.class, remap = false)
public abstract class EffectTossMixin {
    @Inject(method = "onResolveEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$tossEntityCooldown(
            EntityHitResult ray,
            Level level,
            LivingEntity shooter,
            SpellStats stats,
            SpellContext context,
            SpellResolver resolver,
            CallbackInfo ci
    ) {
        if (!ArsGlyphThrottle.tryToss(shooter, level)) {
            ci.cancel();
        }
    }

    @Inject(method = "onResolveBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$tossBlockCooldown(
            BlockHitResult ray,
            Level level,
            LivingEntity shooter,
            SpellStats stats,
            SpellContext context,
            SpellResolver resolver,
            CallbackInfo ci
    ) {
        if (!ArsGlyphThrottle.tryToss(shooter, level)) {
            ci.cancel();
        }
    }

    @Inject(method = "getStackSize", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$capTossStackSize(SpellStats stats, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ArsGlyphPerfConfig.capTossStackSize(cir.getReturnValueI()));
    }
}
