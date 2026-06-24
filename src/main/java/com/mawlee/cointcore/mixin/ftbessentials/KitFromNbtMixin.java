package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.ftbessentials.KitStackOverflowCodec;
import dev.ftb.mods.ftbessentials.kit.Kit;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = Kit.class, remap = false)
public abstract class KitFromNbtMixin {
    @Inject(
            method = "fromNBT",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void cointcore$fromNbt(String kitName, CompoundTag tag, HolderLookup.Provider provider, CallbackInfoReturnable<Kit> cir) {
        var items = new java.util.ArrayList<ItemStack>();
        var list = tag.getList("items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        list.forEach(element -> {
            if (element instanceof CompoundTag itemTag) {
                KitStackOverflowCodec.decodeStack(provider, itemTag).ifPresent(items::add);
            }
        });
        cir.setReturnValue(new Kit(kitName, items, tag.getLong("cooldown"), tag.getBoolean("auto_grant")));
    }
}
