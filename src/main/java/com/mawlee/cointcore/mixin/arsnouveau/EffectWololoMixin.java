package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectWololo;
import com.mawlee.cointcore.ars.ArsGlyphThrottle;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EffectWololo.class, remap = false)
public abstract class EffectWololoMixin {
    @Inject(method = "onResolveEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$wololoEntityCooldown(
            EntityHitResult ray,
            Level level,
            LivingEntity shooter,
            SpellStats stats,
            SpellContext context,
            SpellResolver resolver,
            CallbackInfo ci
    ) {
        if (!ArsGlyphThrottle.tryWololo(shooter, level)) {
            ci.cancel();
        }
    }

    @Inject(method = "onResolveBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$wololoBlockCooldown(
            BlockHitResult ray,
            Level level,
            LivingEntity shooter,
            SpellStats stats,
            SpellContext context,
            SpellResolver resolver,
            CallbackInfo ci
    ) {
        if (!ArsGlyphThrottle.tryWololo(shooter, level)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "onResolveBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    remap = true
            ),
            remap = false
    )
    private boolean cointcore$skipUnchangedWololo(Level level, BlockPos pos, BlockState newState) {
        if (ArsGlyphPerfConfig.isWololoSkipUnchanged()) {
            BlockState current = level.getBlockState(pos);
            if (current.equals(newState)) {
                return false;
            }
        }
        return level.setBlockAndUpdate(pos, newState);
    }
}
