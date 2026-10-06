package com.mawlee.cointcore.item;

import org.jetbrains.annotations.Nullable;

/**
 * Implemented on {@link net.minecraft.world.entity.item.ItemEntity} by mixin.
 */
public interface ItemPileHolder {
    @Nullable
    ItemPile cointcore$getPile();

    void cointcore$setPile(@Nullable ItemPile pile);

    long cointcore$getShownTotal();

    void cointcore$setShownTotal(long total);
}
