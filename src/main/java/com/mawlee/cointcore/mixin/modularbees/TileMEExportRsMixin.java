package com.mawlee.cointcore.mixin.modularbees;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.mawlee.cointcore.modularbees.ExportBuffer;
import com.mawlee.cointcore.modularbees.RsHatchHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * With an RS link the hatch takes everything the core produces into its own unlimited buffer at once (so the
 * centrifuge or hive never stalls on full output slots), then flushes that buffer to storage on the batch schedule.
 */
@Mixin(targets = "com.glodblock.github.modularbees.xmod.ae.tileentities.TileMEExport", remap = false)
public abstract class TileMEExportRsMixin {
    @Unique private final ExportBuffer cointcore$buffer = new ExportBuffer();

    @Shadow protected abstract void extractOutputs();
    @Shadow protected abstract boolean checkFilter(ItemStack stack);

    @Unique
    private RsHatchHost cointcore$host() {
        return (RsHatchHost) this;
    }

    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$batchRsExport(Level level, BlockState state, CallbackInfo callback) {
        var host = cointcore$host();
        var connection = host.cointcore$rsHatch();
        boolean due = connection.isDue();
        // Also runs after the RS link is gone, so a buffer left over is handed to whatever storage remains.
        if (due && !cointcore$buffer.isEmpty() && cointcore$buffer.flush(host.getMEStorage(), host.getSource())) {
            host.saveChanges();
        }
        if (!connection.isPreferred()) return;
        callback.cancel();
        if (host.isActive() && due) extractOutputs();
    }

    @Inject(method = "sendToMENetwork", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$bufferItems(List<ItemStack> sending, CallbackInfo callback) {
        if (!cointcore$host().cointcore$rsHatch().isPreferred()) return;
        callback.cancel();
        boolean changed = false;
        for (ItemStack stack : sending) {
            if (stack.isEmpty() || !checkFilter(stack)) continue;
            cointcore$buffer.add(AEItemKey.of(stack), stack.getCount());
            stack.setCount(0);
            changed = true;
        }
        if (changed) cointcore$host().saveChanges();
    }

    @Inject(method = "sendToMENetworkFluid", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$bufferFluids(List<FluidStack> filling, CallbackInfo callback) {
        if (!cointcore$host().cointcore$rsHatch().isPreferred()) return;
        callback.cancel();
        boolean changed = false;
        for (FluidStack stack : filling) {
            if (stack.isEmpty()) continue;
            cointcore$buffer.add(AEFluidKey.of(stack), stack.getAmount());
            stack.setAmount(0);
            changed = true;
        }
        if (changed) cointcore$host().saveChanges();
    }

    @Inject(method = "saveTag", at = @At("TAIL"), require = 1)
    private void cointcore$saveBuffer(CompoundTag data, HolderLookup.Provider provider, CallbackInfo callback) {
        cointcore$buffer.save(data, provider);
    }

    @Inject(method = "loadTag", at = @At("TAIL"), require = 1)
    private void cointcore$loadBuffer(CompoundTag data, HolderLookup.Provider provider, CallbackInfo callback) {
        cointcore$buffer.load(data, provider);
    }

    /** Broken hatch: hand the buffer to storage if it is still reachable, drop the items that are left. */
    public void addInventoryDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        var host = cointcore$host();
        cointcore$buffer.flush(host.getMEStorage(), host.getSource());
        cointcore$buffer.drainItemsInto(drops);
    }
}
