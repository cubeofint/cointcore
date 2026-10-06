package com.mawlee.cointcore.mixin.minecraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * {@link ItemEntity} never collides with other entities ({@code canBeCollidedWith} is false),
 * but {@link Entity#move} still scans the whole 16³ section via {@code getEntityCollisions}.
 * Skip that scan. Block collision in {@code collideBoundingBox} stays in place.
 */
@Mixin(Entity.class)
public abstract class ItemEntityMoveCollisionMixin {
    @Redirect(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List<VoxelShape> cointcore$skipItemEntityCollisionScan(Level level, Entity entity, AABB box) {
        if (entity instanceof ItemEntity) {
            return List.of();
        }
        return level.getEntityCollisions(entity, box);
    }
}
