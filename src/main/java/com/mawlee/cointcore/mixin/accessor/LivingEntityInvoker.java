package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("dropAllDeathLoot")
    void cointcore$invokeDropAllDeathLoot(ServerLevel level, DamageSource damageSource);
}
