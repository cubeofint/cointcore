package com.mawlee.cointcore.mixin.pipez;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.PipezArmoryFastRejectConfig;
import com.mawlee.cointcore.pipez.ArmoryFreeSlotHint;
import com.mawlee.cointcore.pipez.PipezArmoryFastReject;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Soft mixin on Pipez 1.2.31 {@code ItemPipeType#insertOrdered} / {@code insertEqually}.
 * When dest is a Functional Storage Armory Cabinet and the stack is not certified,
 * skip {@code ItemHandlerHelper.insertItem}'s 4096-slot walk. Certified single items go
 * straight to a hinted free slot ({@link ArmoryFreeSlotHint}) instead of walking occupied slots.
 *
 * <p>Does not patch {@code ItemHandlerHelper} globally. Compatible with other
 * mixins on {@code ItemPipeType} (including invasiveopts) via MixinExtras wrap chaining.
 */
@Pseudo
@Mixin(
        targets = "de.maxhenkel.pipez.blocks.tileentity.types.ItemPipeType",
        remap = false
)
public abstract class ItemPipeTypeArmoryFastRejectMixin {
    @WrapOperation(
            method = {"insertOrdered", "insertEqually"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/items/ItemHandlerHelper;insertItem(Lnet/neoforged/neoforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = false,
            require = 0
    )
    private ItemStack cointcore$fastRejectUncertifiedArmoryInsert(
            IItemHandler dest,
            ItemStack stack,
            boolean simulate,
            Operation<ItemStack> original
    ) {
        boolean armory = PipezArmoryFastReject.isArmoryCabinet(dest);
        boolean empty = stack == null || stack.isEmpty();
        boolean certified = !armory || empty || dest.isItemValid(0, stack);
        if (PipezArmoryFastReject.shouldSkipInsert(
                PipezArmoryFastRejectConfig.isEnabled(),
                armory,
                empty,
                certified
        )) {
            PipezArmoryFastReject.recordSkippedInsert();
            return stack;
        }
        if (armory && !empty && certified && stack.getCount() == 1 && PipezArmoryFastRejectConfig.isFreeSlotHintEnabled()) {
            return cointcore$insertAtFreeSlot(dest, stack, simulate, original);
        }
        return original.call(dest, stack, simulate);
    }

    /**
     * Certified single item: insert straight into the hinted free slot instead of walking the occupied
     * slots from 0. Falls back to the original walk if the cabinet refuses the hinted slot.
     */
    @Unique
    private static ItemStack cointcore$insertAtFreeSlot(
            IItemHandler dest,
            ItemStack stack,
            boolean simulate,
            Operation<ItemStack> original
    ) {
        int size = dest.getSlots();
        int slot = ArmoryFreeSlotHint.findFreeSlot(dest, size, i -> dest.getStackInSlot(i).isEmpty(), System.nanoTime());
        if (slot == ArmoryFreeSlotHint.FULL) {
            return stack;
        }
        ItemStack remainder = dest.insertItem(slot, stack, simulate);
        if (remainder.getCount() == stack.getCount()) {
            return original.call(dest, stack, simulate);
        }
        if (!simulate) {
            ArmoryFreeSlotHint.onInserted(dest, slot, size);
        }
        return remainder;
    }
}
