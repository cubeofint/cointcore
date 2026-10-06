package com.mawlee.cointcore.mixin.advancedae;

import com.mawlee.cointcore.config.AdvancedAePerfConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quantum Armor passive cards run every inventory tick. The expensive ones are:
 * <ul>
 *   <li>{@code autoStock} — full {@code MEStorage.getAvailableStacks()}</li>
 *   <li>{@code magnet} — AABB {@code getEntities} for items + XP</li>
 *   <li>{@code autoFeed} — linked-grid extract</li>
 *   <li>{@code recharging} — scan inventory/curios and pull AE power</li>
 * </ul>
 * Returning {@code false} skips energy consume for that tick (see {@code IUpgradeableItem.tickUpgrades}).
 */
@Mixin(targets = "net.pedroksl.advanced_ae.common.items.upgrades.UpgradeCards", remap = false)
public abstract class UpgradeCardsAutoStockMixin {
    @Inject(method = "autoStock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$throttleAutoStock(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cointcore$throttle(level, AdvancedAePerfConfig.getAutoStockIntervalTicks(), cir);
    }

    @Inject(method = "magnet", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$throttleMagnet(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cointcore$throttle(level, AdvancedAePerfConfig.getMagnetIntervalTicks(), cir);
    }

    @Inject(method = "autoFeed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$throttleAutoFeed(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cointcore$throttle(level, AdvancedAePerfConfig.getAutoFeedIntervalTicks(), cir);
    }

    @Inject(method = "recharging", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$throttleRecharging(
            Level level,
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cointcore$throttle(level, AdvancedAePerfConfig.getRechargingIntervalTicks(), cir);
    }

    private static void cointcore$throttle(Level level, int interval, CallbackInfoReturnable<Boolean> cir) {
        if (!AdvancedAePerfConfig.shouldRunThisTick(level, interval)) {
            cir.setReturnValue(false);
        }
    }
}
