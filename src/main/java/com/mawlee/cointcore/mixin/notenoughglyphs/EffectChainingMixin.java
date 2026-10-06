package com.mawlee.cointcore.mixin.notenoughglyphs;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(targets = "alexthw.not_enough_glyphs.common.glyphs.effects.EffectChaining", remap = false)
public abstract class EffectChainingMixin {
    @ModifyArg(
            method = "onResolveBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lalexthw/not_enough_glyphs/common/glyphs/effects/EffectChaining;SearchTargets(Ljava/util/Collection;ILjava/util/function/Function;Ljava/util/function/Function;Ljava/util/function/BiFunction;Ljava/util/function/Predicate;)Ljava/lang/Iterable;"
            ),
            index = 1,
            remap = false
    )
    private int cointcore$capChainBlocks(int maxBlocks) {
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return maxBlocks;
        }
        return Math.min(maxBlocks, ArsGlyphPerfConfig.getChainMaxBlocks());
    }

    @ModifyArg(
            method = "onResolveEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lalexthw/not_enough_glyphs/common/glyphs/effects/EffectChaining;SearchTargets(Ljava/util/Collection;ILjava/util/function/Function;Ljava/util/function/Function;Ljava/util/function/BiFunction;Ljava/util/function/Predicate;)Ljava/lang/Iterable;"
            ),
            index = 1,
            remap = false
    )
    private int cointcore$capChainEntities(int maxEntities) {
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return maxEntities;
        }
        return Math.min(maxEntities, ArsGlyphPerfConfig.getChainMaxEntities());
    }

    @Redirect(
            method = "onResolveBlock",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D"),
            remap = false
    )
    private double cointcore$capBlockSearchRadius(double distance) {
        double radius = Math.ceil(distance);
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return radius;
        }
        return Math.min(radius, ArsGlyphPerfConfig.getChainMaxBlockSearchRadius());
    }

    @Redirect(
            method = "lambda$onResolveEntity$13",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private static <T extends Entity> List<T> cointcore$capEntitySearch(
            Level level,
            Class<T> type,
            AABB box,
            Predicate<? super T> predicate
    ) {
        double maxRadius = ArsGlyphPerfConfig.isEnabled()
                ? ArsGlyphPerfConfig.getChainMaxEntitySearchRadius()
                : Double.MAX_VALUE;
        return level.getEntitiesOfClass(type, limitBox(box, maxRadius), predicate);
    }

    @Redirect(
            method = {"onResolveBlock", "onResolveEntity"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onResolveEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;)V"
            ),
            remap = false
    )
    private void cointcore$budgetChainResolve(SpellResolver resolver, Level level, HitResult hit) {
        if (ArsGlyphPerfConfig.tryConsumeSpellResolve(level)) {
            resolver.onResolveEffect(level, hit);
        }
    }

    private static AABB limitBox(AABB box, double maxRadius) {
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double halfX = Math.min((box.maxX - box.minX) * 0.5D, maxRadius);
        double halfY = Math.min((box.maxY - box.minY) * 0.5D, maxRadius);
        double halfZ = Math.min((box.maxZ - box.minZ) * 0.5D, maxRadius);
        return new AABB(
                centerX - halfX, centerY - halfY, centerZ - halfZ,
                centerX + halfX, centerY + halfY, centerZ + halfZ
        );
    }
}
