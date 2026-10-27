package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.RecentOrdersRepository;
import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepository;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public abstract class RecentOrdersMenuMixin {
    @Shadow @Final protected Inventory playerInventory;
    @Unique private ResourceRepository<GridResource> cointcore$orders;

    @Inject(method = "getRepository", at = @At("RETURN"), cancellable = true, require = 1)
    private void cointcore$prependOrders(CallbackInfoReturnable<ResourceRepository<GridResource>> callback) {
        if (!playerInventory.player.level().isClientSide()) return;
        if (cointcore$orders == null) cointcore$orders = new RecentOrdersRepository(callback.getReturnValue());
        callback.setReturnValue(cointcore$orders);
    }
}
