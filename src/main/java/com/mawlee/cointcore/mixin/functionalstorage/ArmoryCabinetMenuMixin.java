package com.mawlee.cointcore.mixin.functionalstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.config.FunctionalStorageArmorySearchConfig;
import com.mawlee.cointcore.functionalstorage.ArmoryCabinetSearch;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Functional Storage 1.5.8 {@code ArmoryCabinetMenu#matches} calls
 * {@code ItemStack#getTooltipLines} on the dedicated server when the query
 * misses the hover name. Relics then crashes. Search by name + registry id only.
 */
@Pseudo
@Mixin(targets = "com.buuz135.functionalstorage.inventory.ArmoryCabinetMenu", remap = false)
public abstract class ArmoryCabinetMenuMixin {
    @WrapMethod(method = "matches(Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;)Z", remap = false)
    private boolean cointcore$matchesWithoutTooltips(ItemStack stack, String query, Operation<Boolean> original) {
        if (!FunctionalStorageArmorySearchConfig.isEnabled()) {
            return original.call(stack, query);
        }
        return ArmoryCabinetSearch.matches(stack, query);
    }
}
