package com.mawlee.cointcore.mixin.integratedtunnels;

import com.mawlee.cointcore.config.IntegratedTransferPerfConfig;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorageSlotted;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Avoid probing every destination slot when this source slot cannot supply anything. */
@Mixin(targets = "org.cyclops.cyclopscore.ingredient.storage.IngredientStorageHelpers", remap = false)
public abstract class EmptySourceSlotMixin {
    @Inject(method = "moveIngredientsSlotted(Lorg/cyclops/commoncapabilities/api/ingredient/storage/IIngredientComponentStorage;ILorg/cyclops/commoncapabilities/api/ingredient/storage/IIngredientComponentStorage;ILjava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private static <T, M> void cointcore$emptyPrototypeSource(IIngredientComponentStorage<T, M> source, int sourceSlot,
            IIngredientComponentStorage<T, M> destination, int destinationSlot, T instance, M match,
            boolean simulate, CallbackInfoReturnable<T> ci) {
        if (!IntegratedTransferPerfConfig.isEmptySourceFastPath() || sourceSlot < 0 || destinationSlot >= 0) return;
        long amount = source.getComponent().getMatcher().getQuantity(instance);
        if (cointcore$emptySource(source, sourceSlot, destination, destinationSlot, amount))
            ci.setReturnValue(source.getComponent().getMatcher().getEmptyInstance());
    }

    @Inject(method = "moveIngredientsSlotted(Lorg/cyclops/commoncapabilities/api/ingredient/storage/IIngredientComponentStorage;ILorg/cyclops/commoncapabilities/api/ingredient/storage/IIngredientComponentStorage;ILjava/util/function/Predicate;JZZ)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private static <T, M> void cointcore$emptyPredicateSource(IIngredientComponentStorage<T, M> source, int sourceSlot,
            IIngredientComponentStorage<T, M> destination, int destinationSlot, Predicate<T> predicate, long amount,
            boolean exact, boolean simulate, CallbackInfoReturnable<T> ci) {
        if (IntegratedTransferPerfConfig.isEmptySourceFastPath()
                && cointcore$emptySource(source, sourceSlot, destination, destinationSlot, amount))
            ci.setReturnValue(source.getComponent().getMatcher().getEmptyInstance());
    }

    private static <T, M> boolean cointcore$emptySource(IIngredientComponentStorage<T, M> source, int slot,
            IIngredientComponentStorage<T, M> destination, int destinationSlot, long amount) {
        if (slot < 0 || destinationSlot >= 0 || amount <= 0
                || !(source instanceof IIngredientComponentStorageSlotted<T, M> from)
                || !(destination instanceof IIngredientComponentStorageSlotted<T, M> to)
                || slot >= from.getSlots() || to.getSlots() <= 1) return false;
        return source.getComponent().getMatcher().isEmpty(from.extract(slot, amount, true));
    }
}
