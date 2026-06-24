package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.ftbessentials.KitStackOverflowCodec;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Kit.class, remap = false)
public abstract class KitSaveStackMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "saveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$saveStack(ItemStack stack, HolderLookup.Provider provider, CallbackInfoReturnable<SNBTCompoundTag> cir) {
        try {
            SNBTCompoundTag tag = KitStackOverflowCodec.encodeStack(stack, provider);
            tag.singleLine();
            cir.setReturnValue(tag);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save FTB Essentials kit item ({} x {}), capping count to 99: {}",
                    stack.getItem(), stack.getCount(), exception.toString());
            try {
                ItemStack capped = stack.getCount() > 99 ? stack.copyWithCount(99) : stack;
                SNBTCompoundTag tag = SNBTCompoundTag.of((CompoundTag) capped.save(provider));
                tag.singleLine();
                cir.setReturnValue(tag);
            } catch (Exception fallbackException) {
                LOGGER.error("Unable to save FTB Essentials kit item even with count cap, skipping item data", fallbackException);
                cir.setReturnValue(new SNBTCompoundTag());
            }
        }
    }
}
