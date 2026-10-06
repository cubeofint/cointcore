package com.mawlee.cointcore.mixin.evilcraft;

import net.neoforged.neoforge.fluids.FluidStack;
import org.cyclops.evilcraft.network.packet.UpdateWorldSharedTankClientCachePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = UpdateWorldSharedTankClientCachePacket.class, remap = false)
public interface UpdateWorldSharedTankPacketAccessor {
    @Accessor("tankID")
    String cointcore$getTankID();

    @Accessor("tankID")
    void cointcore$setTankID(String tankID);

    @Accessor("fluidStack")
    FluidStack cointcore$getFluidStack();

    @Accessor("fluidStack")
    void cointcore$setFluidStack(FluidStack fluidStack);
}
