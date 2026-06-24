package com.mawlee.cointcore.mixin.arsnouveau;



import com.hollingsworth.arsnouveau.api.spell.SpellContext;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;

import com.hollingsworth.arsnouveau.api.util.SpellUtil;

import com.mawlee.cointcore.claim.ClaimGuard;

import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Redirect;



import java.util.List;



@Mixin(value = com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak.class, remap = false)

public abstract class EffectBreakMixin {

    @Redirect(

            method = "onResolveBlock",

            at = @At(

                    value = "INVOKE",

                    target = "Lcom/hollingsworth/arsnouveau/api/util/SpellUtil;calcAOEBlocks(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;DI)Ljava/util/List;"

            ),

            remap = false

    )

    private List<BlockPos> cointcore$filterAoeBlocks(

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

        if (!ClaimGuard.isAvailable() || !(shooter instanceof ServerPlayer player)) {

            return blocks;

        }



        return blocks.stream()

                .filter(blockPos -> ClaimGuard.canEdit(player, level, blockPos))

                .toList();

    }

}

