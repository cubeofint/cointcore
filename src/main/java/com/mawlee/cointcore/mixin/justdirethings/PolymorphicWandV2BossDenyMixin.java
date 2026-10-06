package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.items.PolymorphicWandV2;
import com.mawlee.cointcore.justdirethings.PolymorphBossGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shift-use copies the looked-at mob. Bosses stay off that list. */
@Mixin(value = PolymorphicWandV2.class, remap = false)
public abstract class PolymorphicWandV2BossDenyMixin {
    @Inject(method = "savePolymorphTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$denyBossSample(ItemStack stack, Player player, LivingEntity entity, CallbackInfo ci) {
        if (!PolymorphBossGuard.isBoss(entity.getType())) {
            return;
        }
        player.displayClientMessage(Component.translatable("justdirethings.invalidpolymorphentity"), true);
        ci.cancel();
    }
}
