package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.item.ItemPile;
import com.mawlee.cointcore.item.ItemPileHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item pile reserve storage on {@link ItemEntity} (see {@link com.mawlee.cointcore.item.ItemPiles}).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPileMixin implements ItemPileHolder {
    @Unique
    private static final String cointcore$PILE_KEY = "CointCorePile";

    @Unique
    @Nullable
    private ItemPile cointcore$pile;

    @Unique
    private long cointcore$shownTotal = -1L;

    @Override
    public @Nullable ItemPile cointcore$getPile() {
        return cointcore$pile;
    }

    @Override
    public void cointcore$setPile(@Nullable ItemPile pile) {
        cointcore$pile = pile;
        cointcore$shownTotal = -1L;
    }

    @Override
    public long cointcore$getShownTotal() {
        return cointcore$shownTotal;
    }

    @Override
    public void cointcore$setShownTotal(long total) {
        cointcore$shownTotal = total;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void cointcore$savePile(CompoundTag tag, CallbackInfo ci) {
        if (cointcore$pile != null && !cointcore$pile.isEmpty()) {
            tag.put(cointcore$PILE_KEY, cointcore$pile.save(((ItemEntity) (Object) this).registryAccess()));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void cointcore$loadPile(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(cointcore$PILE_KEY, Tag.TAG_LIST)) {
            return;
        }
        ItemPile pile = ItemPile.load(
                ((ItemEntity) (Object) this).registryAccess(),
                tag.getList(cointcore$PILE_KEY, Tag.TAG_COMPOUND)
        );
        cointcore$setPile(pile.isEmpty() ? null : pile);
    }
}
