package com.mawlee.cointcore.mixin.evilcraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.cyclops.evilcraft.entity.item.EntityBroom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The broom stays a ride. Nearby-entity scans only fed damage, potion effects and part collisions.
 */
@Mixin(value = EntityBroom.class, remap = false)
public abstract class EntityBroomMixin {

    @Inject(method = "collideWithNearbyEntities", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipParkedEntityScan(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            ),
            remap = true
    )
    private List<Entity> cointcore$skipMountedHits(Level level, Entity entity, AABB box) {
        return List.of();
    }
}
