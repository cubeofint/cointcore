package com.mawlee.cointcore.mixin.arsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.mawlee.cointcore.ars.CrushRecipeCache;
import com.mawlee.cointcore.claim.ClaimGuard;
import com.mawlee.cointcore.config.ArsGlyphPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = com.hollingsworth.arsnouveau.common.spell.effect.EffectCrush.class, remap = false)
public abstract class EffectCrushMixin {
    @Redirect(
            method = {"onResolveBlock", "crushItems"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager;getAllRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;",
                    remap = true
            ),
            remap = false
    )
    private static List<?> cointcore$cacheCrushRecipes(RecipeManager manager, RecipeType<?> type) {
        return CrushRecipeCache.getAll(manager, type);
    }

    @Redirect(
            method = "onResolveBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hollingsworth/arsnouveau/api/util/SpellUtil;calcAOEBlocks(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;DI)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<BlockPos> cointcore$capAndFilterAoe(
            LivingEntity entity,
            BlockPos pos,
            BlockHitResult hit,
            double aoe,
            int pierce,
            BlockHitResult ray,
            Level level,
            LivingEntity shooter,
            SpellStats stats,
            SpellContext context,
            SpellResolver resolver
    ) {
        List<BlockPos> blocks = SpellUtil.calcAOEBlocks(entity, pos, hit, aoe, pierce);
        if (!ArsGlyphPerfConfig.isEnabled()) {
            return blocks;
        }

        if (ArsGlyphPerfConfig.isCrushClaimFilter()
                && ClaimGuard.isAvailable()
                && shooter instanceof ServerPlayer player) {
            blocks = blocks.stream()
                    .filter(blockPos -> ClaimGuard.canEdit(player, level, blockPos))
                    .toList();
        }

        int max = ArsGlyphPerfConfig.getCrushMaxAoeBlocks();
        if (blocks.size() <= max) {
            return blocks;
        }
        return new ArrayList<>(blocks.subList(0, max));
    }
}
