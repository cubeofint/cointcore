package com.mawlee.cointcore.mixin.client;

import com.mawlee.cointcore.seeinvisible.SeeInvisibleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Staff with {@code cointcore.vanish.see} pierce game invisibility and get a local outline.
 */
@Mixin(Entity.class)
public abstract class EntitySeeInvisibleMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void cointcore$staffSeeInvisible(Player viewer, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player subject) || subject == viewer || viewer == null) {
            return;
        }
        if (!SeeInvisibleClient.canSeeInvisible(viewer)) {
            return;
        }
        if (self.isInvisible()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void cointcore$staffOutlineInvisible(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player) || !self.isInvisible()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player local = minecraft.player;
        if (local == null || self == local || !SeeInvisibleClient.canSeeInvisible(local)) {
            return;
        }

        cir.setReturnValue(true);
    }
}
