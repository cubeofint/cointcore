package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.support.amount.AbstractAmountScreen;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractAmountScreen.class, remap = false)
public interface PreviewAmountFieldAccessor {
    @Accessor("amountField") EditBox cointcore$amountField();
}
