package com.mawlee.cointcore.mixin.relics;

import com.mawlee.cointcore.seeinvisible.SeeInvisibleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Relics {@code VanishingEffect} cancels living render entirely; skip that for staff who may pierce invisibility.
 */
@Mixin(targets = "it.hurts.sskirillss.relics.effects.VanishingEffect$ClientEvents", remap = false)
public abstract class RelicsVanishingRenderMixin {
    @Inject(method = "onEntityRender", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$allowStaffSeeVanishing(RenderLivingEvent.Pre<?, ?> event, CallbackInfo ci) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Player local = Minecraft.getInstance().player;
        if (local == null || entity == local || !SeeInvisibleClient.canSeeInvisible(local)) {
            return;
        }

        ci.cancel();
    }
}
