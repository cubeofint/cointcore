package com.mawlee.cointcore.mixin.relics;

import com.mawlee.cointcore.config.RelicsBackpackScanConfig;
import com.mawlee.cointcore.relics.RelicsBackpackScanPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

/**
 * Relics injects into Sophisticated Backpacks {@code inventoryTick} and walks every nested slot
 * for {@code IRelicItem} every tick via {@code relics$tickBackpackContents}. Cancel that merged
 * helper when configured; curios/inventory relics keep working.
 *
 * <p>Priority {@code 1100} applies after Relics' default-priority mixin so the unique method exists.
 * {@code require = 0} keeps prepare/apply soft if Relics changes the helper name.
 */
@Mixin(value = BackpackItem.class, priority = 1100, remap = false)
public abstract class RelicsBackpackScanMixin {
    @Inject(
            method = "relics$tickBackpackContents",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void cointcore$gateBackpackScan(
            ItemStack itemStack,
            Level level,
            Entity entity,
            int itemSlot,
            Set<UUID> visited,
            int depth,
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
