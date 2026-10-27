package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternIntegration;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.ultramega.universalgrid.common.gui.view.GridTypes;
import com.ultramega.universalgrid.common.wirelessuniversalgrid.WirelessUniversalGridItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WirelessUniversalGridItem.class, remap = false)
public abstract class UniversalGridPatternModeMixin {
    @Inject(method = "openGridType", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$replacePlainGrid(ServerPlayer player, ItemStack stack, SlotReference reference,
                                           GridTypes mode, CallbackInfo callback) {
        if (mode == GridTypes.WIRELESS_GRID) {
            WirelessPatternIntegration.openFromDevice(player, stack, reference);
            callback.cancel();
        }
    }

}
