package com.mawlee.cointcore.mixin.ae2wtlib;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The universal terminal calls this once per player per tick. Restock walks the whole
 * inventory and the ME cache, and the magnet scans item entities out to 16 blocks.
 * Both still run, on a staggered 10-tick cadence.
 */
@Mixin(targets = "de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler", remap = false)
public abstract class MagnetHandlerThrottleMixin {
    private static final int INTERVAL = 10;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$throttleMagnet(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        if (player.level() == null) {
            return;
        }
        int phase = Math.floorMod(player.getId(), INTERVAL);
        if (Math.floorMod(player.level().getGameTime(), INTERVAL) != phase) {
            ci.cancel();
        }
    }
}
