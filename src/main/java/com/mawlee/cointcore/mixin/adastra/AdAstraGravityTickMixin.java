package com.mawlee.cointcore.mixin.adastra;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Marker on the classes Ad Astra's {@code GravityEntityMixin} actually targets.
 * The gravity skip cannot {@code @Inject} into {@code adastra$tick}: Mixin
 * renames that handler before injection targets are validated.
 * {@code CointCoreMixinPlugin} patches the merged handler after apply.
 */
@Mixin({AbstractMinecart.class, ItemEntity.class, PrimedTnt.class})
public abstract class AdAstraGravityTickMixin {
}
