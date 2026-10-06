package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAgeAccessor {
    @Accessor("age")
    int cointcore$getAge();

    @Accessor("age")
    void cointcore$setAge(int age);
}
