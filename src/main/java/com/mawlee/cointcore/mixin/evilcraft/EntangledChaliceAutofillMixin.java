package com.mawlee.cointcore.mixin.evilcraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.cyclops.evilcraft.item.ItemEntangledChalice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An activated chalice walks the player's extended inventory every tick looking for
 * single stacks to fill. The same pass still runs, once every 10 ticks.
 */
@Mixin(value = ItemEntangledChalice.class, remap = false)
public abstract class EntangledChaliceAutofillMixin {
    private static final int INTERVAL = 10;

    @Inject(method = "autofill", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttleAutofill(
            int slot,
            IFluidHandlerItem handler,
            Level level,
            Entity entity,
            CallbackInfo ci
    ) {
        if (level == null || entity == null) {
            return;
        }
        int phase = Math.floorMod(entity.getId(), INTERVAL);
        if (Math.floorMod(level.getGameTime(), INTERVAL) != phase) {
            ci.cancel();
        }
    }
}
