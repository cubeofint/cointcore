package com.mawlee.cointcore.mixin.advancedae;

import appeng.api.stacks.GenericStack;
import com.llamalad7.mixinextras.sugar.Local;
import com.mawlee.cointcore.advancedae.AutoStockInventoryCheck;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** AdvancedAE 1.6.12: skip a full ME inventory enumeration when no inventory adjustment is needed. */
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.items.upgrades.UpgradeCards", remap = false)
public abstract class AutoStockIdleScanMixin {
    @Inject(method = "autoStock", at = @At(value = "INVOKE",
            target = "Lappeng/api/storage/MEStorage;getAvailableStacks()Lappeng/api/stacks/KeyCounter;"),
            cancellable = true, require = 1)
    private static void cointcore$skipBalancedStockScan(Level level, Player player, ItemStack armor,
            CallbackInfoReturnable<Boolean> cir, @Local List<GenericStack> filter) {
        if (!level.isClientSide() && Ae2PerfConfig.isAdvancedAeAutoStockIdleFastPath()
                && AutoStockInventoryCheck.isBalanced(filter, player.getInventory())) {
            cir.setReturnValue(false);
        }
    }
}
