package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.brandonscore.inventory.InventoryDynamic;
import com.brandon3055.draconicevolution.items.equipment.IModularMiningTool;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IModularMiningTool.class, remap = false)
public interface ModularMiningToolMixin {
    @Inject(method = "breakAOEBlock", at = @At("HEAD"), cancellable = true)
    private void cointcore$guardBreakAoeBlock(
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            float blockStrength,
            InventoryDynamic inventory,
            boolean dropInInv,
            CallbackInfo ci
    ) {
        if (ClaimGuard.isAvailable() && !ClaimGuard.canEdit(player, level, pos)) {
            ci.cancel();
        }
    }
}
