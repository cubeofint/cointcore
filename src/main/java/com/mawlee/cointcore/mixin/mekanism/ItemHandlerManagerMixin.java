package com.mawlee.cointcore.mixin.mekanism;

import com.mawlee.cointcore.config.MekanismPerfConfig;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.ISidedItemHandler;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.resolver.manager.CapabilityHandlerManager;
import mekanism.common.capabilities.resolver.manager.ItemHandlerManager;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

/** Specialize the inherited generic getter without caching dynamic slot lists or side permissions. */
@Mixin(value = ItemHandlerManager.class, remap = false)
public abstract class ItemHandlerManagerMixin
        extends CapabilityHandlerManager<IInventorySlotHolder, IInventorySlot, IItemHandler, ISidedItemHandler> {
    protected ItemHandlerManagerMixin() {
        super(null, null, null, null, null);
    }

    // ItemHandlerManager inherits this method. Its constructor always supplies
    // IInventorySlotHolder::getInventorySlots as the generic container getter.
    @Override
    public List<IInventorySlot> getContainers(@Nullable Direction side) {
        if (!MekanismPerfConfig.isDirectItemContainerLookup()) return super.getContainers(side);
        return canHandle() ? holder.getInventorySlots(side) : Collections.emptyList();
    }
}
