package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.spawner.SpawnerLootCapture;
import com.mawlee.cointcore.vanish.VanishInteractionTracker;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V", ordinal = 1))
    private void cointcore$clearInteractionOnMove(MoverType type, Vec3 pos, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && !player.hasContainerOpen()) {
            VanishInteractionTracker.clearInteraction(player);
        }
    }

    @Inject(method = "setGlowingTag", at = @At("HEAD"), cancellable = true)
    private void cointcore$preventGlowingWhileVanished(boolean glowing, CallbackInfo ci) {
        if (glowing && (Object) this instanceof ServerPlayer player && VanishManager.isVanished(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void cointcore$hideGlowWhileVanished(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player
                && !player.level().isClientSide()
                && VanishManager.isVanished(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void cointcore$captureLootDrop(ItemStack stack, CallbackInfoReturnable<ItemEntity> cir) {
        if (SpawnerLootCapture.isActive()) {
            SpawnerLootCapture.capture(stack);
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void cointcore$captureLootDropWithOffset(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir) {
        if (SpawnerLootCapture.isActive()) {
            SpawnerLootCapture.capture(stack);
            cir.setReturnValue(null);
        }
    }
}
