package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import org.jetbrains.annotations.Nullable;

@Mixin(LivingEntity.class)
public interface LivingEntityHurtByPlayerAccessor {
    @Accessor("lastHurtByPlayer")
    void cointcore$setLastHurtByPlayer(@Nullable Player player);

    @Accessor("lastHurtByPlayerTime")
    void cointcore$setLastHurtByPlayerTime(int time);
}
