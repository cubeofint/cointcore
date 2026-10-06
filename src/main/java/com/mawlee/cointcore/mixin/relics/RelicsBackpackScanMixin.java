package com.mawlee.cointcore.mixin.relics;

import com.mawlee.cointcore.config.RelicsBackpackScanConfig;
import com.mawlee.cointcore.relics.RelicsBackpackScanPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Relics injects into Sophisticated Backpacks {@code inventoryTick} and walks every nested slot
 * for {@code IRelicItem} every tick. Cancel that when configured; curios/inventory relics keep working.
 */
@Mixin(targets = "it.hurts.sskirillss.relics.mixin.compat.sophisticatedbackpacks.BackpackItemMixin", remap = false)
public abstract class RelicsBackpackScanMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$gateBackpackScan(
            ItemStack itemStack,
            Level level,
            Entity entity,
            int itemSlot,
            boolean isSelected,
            CallbackInfo ci
    ) {
        long gameTime = level != null ? level.getGameTime() : 0L;
        RelicsBackpackScanPolicy.Mode mode = switch (RelicsBackpackScanConfig.getMode()) {
            case SKIP_NESTED -> RelicsBackpackScanPolicy.Mode.SKIP_NESTED;
            case THROTTLE -> RelicsBackpackScanPolicy.Mode.THROTTLE;
        };
        if (!RelicsBackpackScanPolicy.shouldScanNested(
                RelicsBackpackScanConfig.isEnabled(),
                mode,
                RelicsBackpackScanConfig.getScanIntervalTicks(),
                gameTime
        )) {
            ci.cancel();
        }
    }
}
