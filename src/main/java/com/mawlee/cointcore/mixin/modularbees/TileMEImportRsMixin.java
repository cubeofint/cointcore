package com.mawlee.cointcore.mixin.modularbees;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import com.glodblock.github.modularbees.common.inventory.MBBigItemInventory;
import com.glodblock.github.modularbees.common.inventory.MBItemInventory;
import com.mawlee.cointcore.modularbees.ImportAmountHost;
import com.mawlee.cointcore.modularbees.ImportStorageInventory;
import com.mawlee.cointcore.modularbees.RsHatchHost;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(targets = "com.glodblock.github.modularbees.xmod.ae.tileentities.TileMEImport", remap = false)
public abstract class TileMEImportRsMixin implements ImportAmountHost {
    @Unique private static final Logger COINTCORE$LOGGER = LogUtils.getLogger();
    /** A broken hatch drops at most this many stacks of what its network could not take back; the rest is logged and lost. */
    @Unique private static final int COINTCORE$MAX_DROPPED_STACKS = 128;
    @Unique private static final String COINTCORE$AMOUNTS_TAG = "cointcore_amounts";

    @Unique private final int[] cointcore$amounts = new int[SLOTS];

    @Shadow @Final protected MBItemInventory config;
    @Shadow @Final protected MBItemInventory storage;
    @Shadow protected abstract void updateStorage();

    @Override
    public int cointcore$amount(int slot) {
        return slot >= 0 && slot < SLOTS ? cointcore$amounts[slot] : 0;
    }

    @Override
    public void cointcore$setAmount(int slot, int amount) {
        if (slot < 0 || slot >= SLOTS || cointcore$amounts[slot] == Math.max(0, amount)) return;
        cointcore$amounts[slot] = Math.max(0, amount);
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide) ((RsHatchHost) this).saveChanges();
    }

    // The buffer's per-slot limit follows the hatch's amounts (up to Integer.MAX_VALUE) instead of 8 stacks.
    @Redirect(method = "<init>", at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/level/block/entity/BlockEntity;II)Lcom/glodblock/github/modularbees/common/inventory/MBBigItemInventory;"),
            require = 1)
    private MBBigItemInventory cointcore$sizedStorage(BlockEntity host, int multiplier, int size) {
        return new ImportStorageInventory(host, multiplier, size, this);
    }

    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$batchRsImport(Level level, BlockState state, CallbackInfo callback) {
        var connection = ((RsHatchHost) this).cointcore$rsHatch();
        if (!connection.isPreferred()) return;
        callback.cancel();
        if (((RsHatchHost) this).isActive() && connection.isDue()) updateStorage();
    }

    /** ModularBees' refill rule with the slot's own target: positive = fetch, negative = send back. */
    @Inject(method = "getDelta", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$delta(int slot, CallbackInfoReturnable<Integer> callback) {
        ItemStack stored = storage.getStackInSlot(slot);
        ItemStack wanted = config.getStackInSlot(slot);
        int delta;
        if (wanted.isEmpty()) {
            delta = -stored.getCount();
        } else if (stored.isEmpty()) {
            delta = cointcore$target(slot, wanted);
        } else if (ItemStack.isSameItemSameComponents(stored, wanted)) {
            delta = cointcore$target(slot, wanted) - stored.getCount();
        } else {
            delta = -stored.getCount();
        }
        callback.setReturnValue(delta);
    }

    @Inject(method = "saveTag", at = @At("TAIL"), require = 1)
    private void cointcore$saveAmounts(CompoundTag data, HolderLookup.Provider provider, CallbackInfo callback) {
        if (Arrays.stream(cointcore$amounts).anyMatch(amount -> amount > 0)) {
            data.putIntArray(COINTCORE$AMOUNTS_TAG, Arrays.copyOf(cointcore$amounts, SLOTS));
        }
    }

    @Inject(method = "loadTag", at = @At("TAIL"), require = 1)
    private void cointcore$loadAmounts(CompoundTag data, HolderLookup.Provider provider, CallbackInfo callback) {
        int[] saved = data.getIntArray(COINTCORE$AMOUNTS_TAG);
        Arrays.fill(cointcore$amounts, 0);
        System.arraycopy(saved, 0, cointcore$amounts, 0, Math.min(saved.length, SLOTS));
    }

    /**
     * Broken hatch: hand the buffer back to the network, then drop what is left as normal stacks. The buffer can
     * hold billions of items, so drops are capped to keep the server from spawning that many item entities.
     */
    @Inject(method = "addInventoryDrops", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$safeDrops(Level level, BlockPos pos, List<ItemStack> drops, CallbackInfo callback) {
        callback.cancel();
        var host = (RsHatchHost) this;
        var network = host.getMEStorage();
        int droppedStacks = 0;
        long lost = 0;
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            long left = stack.getCount();
            if (network != null) {
                left -= network.insert(AEItemKey.of(stack), left, Actionable.MODULATE, host.getSource());
            }
            int max = Math.max(1, stack.getMaxStackSize());
            while (left > 0 && droppedStacks < COINTCORE$MAX_DROPPED_STACKS) {
                int count = (int) Math.min(max, left);
                drops.add(stack.copyWithCount(count));
                left -= count;
                droppedStacks++;
            }
            lost += left;
            storage.setStackInSlot(slot, ItemStack.EMPTY);
        }
        if (lost > 0) {
            COINTCORE$LOGGER.warn("ME import hatch at {} was broken and its network could not take the whole buffer; {} items beyond {} dropped stacks were discarded",
                    pos.toShortString(), lost, COINTCORE$MAX_DROPPED_STACKS);
        }
    }
}
